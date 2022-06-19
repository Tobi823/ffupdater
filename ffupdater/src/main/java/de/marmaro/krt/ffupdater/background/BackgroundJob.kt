package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.NetworkType.UNMETERED
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.download.AppCache
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
import kotlinx.coroutines.coroutineScope
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
     * The coroutineScope ensures that stopping the BackgroundJob will also stop the app update check, app
     * download and app installation.
     *
     * Result.failure will remove the scheduled job (and that's unwanted).
     * Result.success will execute the job in the next period.
     * Result.retry will retry the job with exponentially increased wait time (30s, 1m, 2m, ...).
     */
    @MainThread
    override suspend fun doWork(): Result = coroutineScope {
        try {
            Log.i(LOG_TAG, "Execute background job for update check.")
            executeBackgroundJob()
        } catch (e: CancellationException) {
            Log.w(LOG_TAG, "Background job failed (CancellationException)", e)
            handleRetryableError(e, RUN_ATTEMPTS_FOR_1HOUR)
        } catch (e: GithubRateLimitExceededException) {
            Log.w(LOG_TAG, "Background job failed (GithubRateLimitExceededException)", e)
            handleRetryableError(e, RUN_ATTEMPTS_FOR_2DAYS)
        } catch (e: NetworkException) {
            Log.w(LOG_TAG, "Background job failed (NetworkException)", e)
            handleRetryableError(e, RUN_ATTEMPTS_FOR_2DAYS)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Background job failed due to an unexpected exception", e)
            showErrorNotification(e)
            Result.success()
        }
    }

    @MainThread
    private suspend fun executeBackgroundJob(): Result {
        areUpdateCheckPreconditionsUnfulfilled()?.let { return it }
        dataStoreHelper.lastBackgroundCheck = LocalDateTime.now()

        // check for updates
        val apps = App.values().asList()
        val outdatedApps = apps.filter { checkForUpdateAndReturnAvailability(it) }
//        val outdatedApps = listOf(App.FIREFOX_RELEASE, App.ICERAVEN)
        if (outdatedApps.isEmpty()) {
            return Result.success()
        }

        // download updates
        areDownloadPreconditionsUnfulfilled()?.let {
            showUpdateNotification(outdatedApps)
            return it
        }
        BackgroundNotificationBuilder.hideDownloadError(context)
        val downloadedUpdates = outdatedApps.filter { downloadUpdateAndReturnAvailability(it) }

        // install updates
        areInstallationPreconditionsUnfulfilled()?.let {
            showUpdateNotification(downloadedUpdates)
            return it
        }
        BackgroundNotificationBuilder.hideInstallationSuccess(context)
        BackgroundNotificationBuilder.hideInstallationError(context)
        downloadedUpdates.forEach { installApplication(it) }
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

        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return Result.retry()
        }

        if (!backgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return Result.retry()
        }

        return null
    }

    private suspend fun checkForUpdateAndReturnAvailability(app: App): Boolean {
        if (app in backgroundSettings.excludedAppsFromUpdateCheck) {
            return false
        }

        if (!app.detail.isInstalled(context)) {
            return false
        }

        val updateCheckResult = app.detail.updateCheckAsync(context).await()
        return updateCheckResult.isUpdateAvailable
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
    private suspend fun downloadUpdateAndReturnAvailability(app: App): Boolean {
        if (!StorageUtil.isEnoughStorageAvailable(context)) {
            Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
            return false
        }

        val appCache = AppCache(app)
        val updateResult = app.detail.updateCheckAsync(context).await()
        val availableResult = updateResult.availableResult
        if (appCache.isAvailable(context, availableResult)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return false
        }

        Log.i(LOG_TAG, "Download update for $app.")
        val downloader = FileDownloader()
        downloader.onProgress = { progressInPercent, totalMB ->
            BackgroundNotificationBuilder.showDownloadIsRunning(context, app, progressInPercent, totalMB)
        }
        BackgroundNotificationBuilder.showDownloadIsRunning(context, app, null, null)

        val file = appCache.getFile(context)
        return try {
            downloader.downloadFileAsync(availableResult.downloadUrl, file).await()
            BackgroundNotificationBuilder.hideDownloadIsRunning(context, app)
            true
        } catch (e: NetworkException) {
            BackgroundNotificationBuilder.hideDownloadIsRunning(context, app)
            BackgroundNotificationBuilder.showDownloadError(context, app, e)
            appCache.delete(context)
            false
        }
    }

    private fun areInstallationPreconditionsUnfulfilled(): Result? {
        if (!DeviceSdkTester.supportsAndroid10()) {
            return Result.success()
        }

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

    private suspend fun installApplication(app: App) {
        // a previous check pretends that this method is called with Android < 10
        // but I have to add this check to make the compiler happy
        if (!DeviceSdkTester.supportsAndroid10()) {
            return
        }

        val appCache = AppCache(app)
        val file = appCache.getFile(context)
        if (!file.exists()) {
            val errorMessage = "AppCache has no cached APK file"
            BackgroundNotificationBuilder.showInstallationError(context, app, -100, errorMessage)
        }

        val installer = BackgroundAppInstaller.create(context, app, file)
        withContext(Dispatchers.Main) {
            val result = installer.installAsync(context).await()
            if (result.success) {
                BackgroundNotificationBuilder.showInstallationSuccess(context, app)
                if (backgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                    appCache.delete(context)
                }
            } else {
                val code = result.errorCode
                val message = result.errorMessage
                BackgroundNotificationBuilder.showInstallationError(context, app, code, message)
                if (backgroundSettings.isDeleteUpdateIfInstallFailed) {
                    appCache.delete(context)
                }
            }
        }
    }

    private suspend fun showUpdateNotification(appsWithUpdates: List<App>) {
        BackgroundNotificationBuilder.hideUpdateIsAvailable(context)
        appsWithUpdates.forEach {
            // updateCheckAsync() should be fast because the result is cached
            val updateCheckResult = it.detail.updateCheckAsync(context).await()
            BackgroundNotificationBuilder.showUpdateIsAvailable(context, it, updateCheckResult)
        }
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
        val message = context.getString(R.string.background_notification__text)
        BackgroundNotificationBuilder.showError(context, e, message)
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