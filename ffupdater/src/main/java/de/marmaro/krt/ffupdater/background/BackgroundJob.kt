package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.NetworkType.UNMETERED
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.background.NotificationBuilder.hideAllFailedBackgroundInstallationNotifications
import de.marmaro.krt.ffupdater.background.NotificationBuilder.hideAllSuccessfulBackgroundInstallationNotifications
import de.marmaro.krt.ffupdater.background.NotificationBuilder.hideDownloadNotification
import de.marmaro.krt.ffupdater.background.NotificationBuilder.showFailedBackgroundInstallationNotification
import de.marmaro.krt.ffupdater.background.NotificationBuilder.showSuccessfulBackgroundInstallationNotification
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.download.AppCache
import de.marmaro.krt.ffupdater.download.AppDownloadStatus
import de.marmaro.krt.ffupdater.download.FileDownloader
import de.marmaro.krt.ffupdater.download.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.download.StorageUtil
import de.marmaro.krt.ffupdater.installer.BackgroundAppInstaller
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper.Installer.ROOT_INSTALLER
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper.Installer.SESSION_INSTALLER
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
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
    private val context = applicationContext
    private val backgroundSettings = BackgroundSettingsHelper(context)
    private val installerSettings = InstallerSettingsHelper(context)
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
        appsWithAvailableUpdates.forEach { downloadUpdate(it) }
        hideDownloadNotification(context)

        if (!DeviceSdkTester.supportsAndroid10()) {
            return Result.success()
        }
        areInstallationPreconditionsUnfulfilled()?.let {
            showUpdateNotification(appsWithAvailableUpdates)
            return it
        }
        hideAllSuccessfulBackgroundInstallationNotifications(context)
        hideAllFailedBackgroundInstallationNotifications(context)
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
        if (!backgroundSettings.isUpdateCheckEnabled) {
            Log.i(LOG_TAG, "Background should be disabled - disable it now.")
            return Result.failure()
        }

        if (AppDownloadStatus.areDownloadsInForegroundActive()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return Result.retry()
        }

        if (!backgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return Result.retry()
        }

        return null
    }

    private suspend fun checkForUpdates(): List<App> {
        val apps = App.values()
            .filter { it !in backgroundSettings.excludedAppsFromUpdateCheck }
            .filter { it.detail.isInstalled(context) }
        val appsWithAvailableUpdates = apps.filter {
            val updateCheckResult = it.detail.updateCheckAsync(context).await()
            updateCheckResult.isUpdateAvailable
        }
        return appsWithAvailableUpdates
    }

    private fun areDownloadPreconditionsUnfulfilled(): Result? {
        if (!backgroundSettings.isDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            return Result.success()
        }

        if (!backgroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(context)) {
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
        if (!StorageUtil.isEnoughStorageAvailable(context)) {
            Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
            return
        }

        val appCache = AppCache(app)
        val updateResult = app.detail.updateCheckAsync(context).await()
        val availableResult = updateResult.availableResult
        if (appCache.isAvailable(context, availableResult)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return
        }

        Log.i(LOG_TAG, "Download update for $app.")
        AppDownloadStatus.backgroundDownloadIsStarted()
        val downloader = FileDownloader()
        downloader.onProgress = { progressInPercent, totalMB ->
            NotificationBuilder.showDownloadNotification(context, app, progressInPercent, totalMB)
        }
        NotificationBuilder.showDownloadNotification(context, app, null, null)

        val file = appCache.getFile(context)
        val result = downloader.downloadFileAsync(context, availableResult.downloadUrl, file).await()

        AppDownloadStatus.backgroundDownloadIsFinished()
        if (!result) {
            appCache.delete(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun areInstallationPreconditionsUnfulfilled(): Result? {
        if (!context.packageManager.canRequestPackageInstalls()) {
            Log.i(LOG_TAG, "Missing installation permission")
            return Result.retry()
        }

        if (!backgroundSettings.isInstallationEnabled) {
            Log.i(LOG_TAG, "Automatic background app installation is not enabled.")
            return Result.success()
        }

        if (installerSettings.getInstaller() == ROOT_INSTALLER) {
            return null
        }

        if (DeviceSdkTester.supportsAndroid12() && installerSettings.getInstaller() == SESSION_INSTALLER) {
            return null
        }

        Log.i(LOG_TAG, "The current installer can not update apps in the background")
        return Result.success()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun installApplication(app: App) {
        val appCache = AppCache(app)
        val file = appCache.getFile(context)
        if (!file.exists()) {
            val errorMessage = "AppCache has no cached APK file"
            showFailedBackgroundInstallationNotification(context, app, -100, errorMessage)
        }

        val installer = BackgroundAppInstaller.create(context, app, file)
        withContext(Dispatchers.Main) {
            val result = installer.installAsync(context).await()
            if (result.success) {
                showSuccessfulBackgroundInstallationNotification(context, app)
                if (backgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                    appCache.delete(context)
                }
            } else {
                val code = result.errorCode
                val message = result.errorMessage
                showFailedBackgroundInstallationNotification(context, app, code, message)
                if (backgroundSettings.isDeleteUpdateIfInstallFailed) {
                    appCache.delete(context)
                }
            }
        }
    }

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        NotificationBuilder.showUpdateNotifications(context, appsWithUpdates)
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
        val message = context.getString(R.string.background_job_failure__notification_text)
        NotificationBuilder.showErrorNotification(context, e, message)
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"

        // retry delays: 15s,30s,1m,2m,4m,8m,16m,32m
        private const val RUN_ATTEMPTS_FOR_1HOUR = 8

        // retry delays: 15s,30s,1m,2m,4m,8m,16m,32m,64m,128m,256m,300m,300m,300m,300m,300m,300m,300m,300m
        private const val RUN_ATTEMPTS_FOR_2DAYS = 19

        /**
         * Should be called when the user minimize the app to make sure that the background update check
         * is running.
         */
        fun initBackgroundUpdateCheck(context: Context) {
            val backgroundSettings = BackgroundSettingsHelper(context)
            if (backgroundSettings.isUpdateCheckEnabled) {
                startBackgroundUpdateCheck(context, backgroundSettings, KEEP)
            } else {
                stopBackgroundUpdateCheck(context)
            }
        }

        /**
         * Should be called when the user changes specific background settings.
         * If value is null, the value from SharedPreferences will be used.
         */
        fun changeBackgroundUpdateCheck(context: Context, _enabled: Boolean?, interval: Duration?) {
            val backgroundSettings = BackgroundSettingsHelper(context)
            val enabled = _enabled ?: backgroundSettings.isUpdateCheckEnabled
            if (enabled) {
                startBackgroundUpdateCheck(context, backgroundSettings, REPLACE, interval)
            } else {
                stopBackgroundUpdateCheck(context)
            }
        }

        private fun startBackgroundUpdateCheck(
            context: Context,
            settings: BackgroundSettingsHelper,
            existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
            _interval: Duration? = null,
        ) {
            val requiredNetworkType = if (settings.isUpdateCheckOnMeteredAllowed) {
                NOT_REQUIRED
            } else {
                UNMETERED
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(requiredNetworkType)

            WorkRequest.MAX_BACKOFF_MILLIS

            val interval = _interval ?: settings.updateCheckInterval
            val minutes = interval.toMinutes()
            val workRequest = PeriodicWorkRequest.Builder(BackgroundJob::class.java, minutes, MINUTES)
                .setConstraints(constraints.build())
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_MANAGER_KEY, existingPeriodicWorkPolicy, workRequest)
        }

        private fun stopBackgroundUpdateCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }
    }
}