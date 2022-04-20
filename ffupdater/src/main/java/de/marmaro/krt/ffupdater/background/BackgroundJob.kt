package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.NetworkType.UNMETERED
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.background.NotificationBuilder.hideAllFailedBackgroundInstallationNotifications
import de.marmaro.krt.ffupdater.background.NotificationBuilder.hideAllSuccessfulBackgroundInstallationNotifications
import de.marmaro.krt.ffupdater.background.NotificationBuilder.showFailedBackgroundInstallationNotification
import de.marmaro.krt.ffupdater.background.NotificationBuilder.showSuccessfulBackgroundInstallationNotification
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.download.AppCache
import de.marmaro.krt.ffupdater.download.AppDownloadStatus
import de.marmaro.krt.ffupdater.download.FileDownloader
import de.marmaro.krt.ffupdater.download.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.download.StorageUtil
import de.marmaro.krt.ffupdater.installer.BackgroundAppInstaller
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.MINUTES

/**
 * This class will call the [WorkManager] to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 *
 * doWork can be interrupted at any time and cause a CancellationException.
 */
class BackgroundJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val settingsHelper = SettingsHelper(context)
    private val dataStoreHelper = DataStoreHelper(context)

    /**
     * If:
     * - airplane mode is enabled
     * - internet is not available
     * - the app is currently downloading an app update
     * then delay the background job execution by 30s, 1m, 2m, 4m, ...
     * <a>https://developer.android.com/reference/androidx/work/BackoffPolicy?hl=en#EXPONENTIAL</a>
     * <a>https://developer.android.com/reference/androidx/work/WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS</a>
     *
     * Result.failure will remove the scheduled job (and that's unwanted).
     * Result.success will execute the job in the next period.
     * Result.retry will retry the job with exponentially increased wait time (30s, 1m, 2m, ...).
     */
    @MainThread
    override suspend fun doWork(): Result {
        return try {
            Log.i(LOG_TAG, "Execute background job for update check.")
            executeBackgroundJob()
        } catch (e: CancellationException) {
            handleRetryableError(e, RUN_ATTEMPTS_FOR_1HOUR)
        } catch (e: GithubRateLimitExceededException) {
            handleRetryableError(e, RUN_ATTEMPTS_FOR_2DAYS)
        } catch (e: NetworkException) {
            handleRetryableError(e, RUN_ATTEMPTS_FOR_2DAYS)
        } catch (e: Exception) {
            showErrorNotification(e)
            Result.success()
        }
    }

    @MainThread
    private suspend fun executeBackgroundJob(): Result {
        areUpdateCheckPreconditionsUnfulfilled()?.let { return it }
        val appsWithAvailableUpdates = checkForUpdates()
        dataStoreHelper.lastBackgroundCheck = LocalDateTime.now()
        if (appsWithAvailableUpdates.isEmpty()) {
            return Result.success()
        }

        areDownloadPreconditionsUnfulfilled()?.let {
            showUpdateNotification(appsWithAvailableUpdates)
            return it
        }
        NotificationBuilder.showDownloadNotification(applicationContext)
        appsWithAvailableUpdates.forEach { downloadUpdate(it) }
        NotificationBuilder.hideDownloadNotification(applicationContext)

        if (!DeviceSdkTester.supportsAndroid10()) {
            return Result.success()
        }
        areInstallationPreconditionsUnfulfilled()?.let {
            showUpdateNotification(appsWithAvailableUpdates)
            return it
        }
        hideAllSuccessfulBackgroundInstallationNotifications(applicationContext)
        hideAllFailedBackgroundInstallationNotifications(applicationContext)
        appsWithAvailableUpdates.forEach {
            installApplication(it)
        }
        return Result.success()
    }

    /**
     * Actually, WorkManager should ensure that most of these conditions are met.
     * But this does not always happen reliably.
     */
    private fun areUpdateCheckPreconditionsUnfulfilled(): Result? {
        if (!settingsHelper.isBackgroundUpdateCheckEnabled) {
            Log.i(LOG_TAG, "Background should be disabled - disable it now.")
            return Result.failure()
        }

        if (AppDownloadStatus.areDownloadsInForegroundActive()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return Result.retry()
        }

        if (!settingsHelper.isBackgroundUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return Result.retry()
        }

        return null
    }

    private suspend fun checkForUpdates(): List<App> {
        val apps = App.values()
            .filter { it !in settingsHelper.excludedAppsFromBackgroundUpdateCheck }
            .filter { it.detail.isInstalled(applicationContext) }
        val appsWithAvailableUpdates = apps.filter {
            val updateCheckResult = it.detail.updateCheck(applicationContext)
            updateCheckResult.isUpdateAvailable
        }
        return appsWithAvailableUpdates
    }

    private fun areDownloadPreconditionsUnfulfilled(): Result? {
        if (!settingsHelper.isBackgroundDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            return Result.success()
        }

        if (!settingsHelper.isBackgroundDownloadOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            Log.i(LOG_TAG, "No unmetered network available for download.")
            return Result.success()
        }

        return null
    }

    /**
     * If the app update is not already been cached, then start the download and wait until the
     * download is finished.
     */
    @MainThread
    private suspend fun downloadUpdate(app: App) {
        if (!StorageUtil.isEnoughStorageAvailable()) {
            Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
            return
        }

        val appCache = AppCache(app)
        val availableResult = app.detail.updateCheck(applicationContext).availableResult
        if (appCache.isAvailable(applicationContext, availableResult)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return
        }

        Log.i(LOG_TAG, "Download update for $app.")
        AppDownloadStatus.backgroundDownloadIsStarted()
        val result = FileDownloader().downloadFile(
            availableResult.downloadUrl,
            appCache.getFile(applicationContext)
        )
        AppDownloadStatus.backgroundDownloadIsFinished()
        if (!result) {
            appCache.delete(applicationContext)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun areInstallationPreconditionsUnfulfilled(): Result? {
        if (!applicationContext.packageManager.canRequestPackageInstalls()) {
            return Result.retry()
        }

        if (!settingsHelper.isBackgroundInstallationEnabled) {
            Log.i(LOG_TAG, "Automatic background app installation is not enabled.")
            return Result.success()
        }

        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun installApplication(app: App) {
        val appCache = AppCache(app)
        val file = appCache.getFile(applicationContext)
        if (!file.exists()) {
            val errorMessage = "AppCache has no cached APK file"
            showFailedBackgroundInstallationNotification(applicationContext, app, -100, errorMessage)
        }

        BackgroundAppInstaller.create(applicationContext, app, file).use { installer ->
            withContext(Dispatchers.Main) {
                val result = installer.installAsync().await()
                if (result.success) {
                    showSuccessfulBackgroundInstallationNotification(applicationContext, app)
                } else {
                    val code = result.errorCode
                    val message = result.errorMessage
                    showFailedBackgroundInstallationNotification(applicationContext, app, code, message)
                }
            }
        }
    }

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        NotificationBuilder.showUpdateNotifications(applicationContext, appsWithUpdates)
    }

    private fun handleRetryableError(e: Exception, maxRetries: Int): Result {
        if (runAttemptCount <= maxRetries) {
            Log.i(LOG_TAG, "Retry background job.", e)
            return Result.retry()
        }

        showErrorNotification(e)
        return Result.success()
    }

    private fun showErrorNotification(e: Exception) {
        val message = applicationContext.getString(R.string.background_job_failure__notification_text)
        NotificationBuilder.showErrorNotification(applicationContext, e, message)
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"

        // retry delays: 15s,30s,1m,2m,4m,8m,16m,32m
        private const val RUN_ATTEMPTS_FOR_1HOUR = 8

        // retry delays: 15s,30s,1m,2m,4m,8m,16m,32m,64m,128m,256m,300m,300m,300m,300m,300m,300m,300m,300m
        private const val RUN_ATTEMPTS_FOR_2DAYS = 19

        fun startOrStopBackgroundUpdateCheck(context: Context) {
            if (SettingsHelper(context).isBackgroundUpdateCheckEnabled) {
                startBackgroundUpdateCheck(context)
            } else {
                stopBackgroundUpdateCheck(context)
            }
        }

        private fun startBackgroundUpdateCheck(context: Context) {
            val settings = SettingsHelper(context)
            val meteredAllowed = settings.isBackgroundUpdateCheckOnMeteredAllowed
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(if (meteredAllowed) NOT_REQUIRED else UNMETERED)

            WorkRequest.MAX_BACKOFF_MILLIS

            val interval = settings.backgroundUpdateCheckInterval.toMinutes()
            val workRequest = PeriodicWorkRequest.Builder(BackgroundJob::class.java, interval, MINUTES)
                .setConstraints(constraints.build())
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, workRequest)
        }

        private fun stopBackgroundUpdateCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }
    }
}