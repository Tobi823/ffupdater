package de.marmaro.krt.ffupdater

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.NetworkType.UNMETERED
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.background.RecoverableBackgroundException
import de.marmaro.krt.ffupdater.background.UnrecoverableBackgroundException
import de.marmaro.krt.ffupdater.device.DeviceSdkTester.supportsAndroid10
import de.marmaro.krt.ffupdater.device.DeviceSdkTester.supportsAndroid12
import de.marmaro.krt.ffupdater.installer.BackgroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.ROOT_INSTALLER
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.storage.AppCache
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit.MINUTES

/**
 * BackgroundJob will be regularly called by the AndroidX WorkManager to:
 * - check for app updates
 * - download them
 * - install them
 *
 * Depending on the device and the settings from the user not all steps will be executed.
 */
class BackgroundJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val context = applicationContext
    private val backgroundSettings = BackgroundSettingsHelper(context)
    private val installerSettings = InstallerSettingsHelper(context)
    private val dataStoreHelper = DataStoreHelper(context)

    /**
     * Execute the logic for update checking, downloading and installation.
     *
     * If an common exception (like CancellationException, GithubRateLimitExceededException or
     * NetworkException) occurs, then retry it later.
     * If after 24 retries (five days) the background check still fails, then show a notification and
     * retry in the next regular execution interval.
     *
     * If an uncommon exception (anything else) occurs, then show a notification and retry in the next
     * regular execution interval.
     *
     * I can't return Result.error() because even if an unknown exception occurs, I want that BackgroundJob
     * is still regularly executed.
     * But Result.error() will remove BackgroundJob from the WorkManager job schedule.
     */
    @MainThread
    override suspend fun doWork(): Result = coroutineScope {
        try {
            Log.i(LOG_TAG, "Execute background job for update check.")
            checkDownloadAndInstallApps()
        } catch (e: CancellationException) {
            val wrappedException = RecoverableBackgroundException(e)
            Log.w(LOG_TAG, "Background job failed (CancellationException)", wrappedException)
            handleRecoverableError(wrappedException)
        } catch (e: NetworkException) {
            val wrappedException = RecoverableBackgroundException(e)
            Log.w(LOG_TAG, "Background job failed (NetworkException)", wrappedException)
            handleRecoverableError(wrappedException)
        } catch (e: Exception) {
            val wrappedException = UnrecoverableBackgroundException(e)
            Log.e(LOG_TAG, "Background job failed due to an unexpected exception", wrappedException)
            handleUnrecoverableError(wrappedException)
        }
    }

    private fun handleRecoverableError(e: Exception): Result {
        return if (runAttemptCount < RUN_ATTEMPTS_FOR_MAX_5DAYS) {
            Log.i(LOG_TAG, "Retry background job.", e)
            Result.retry()
        } else {
            BackgroundNotificationBuilder.showLongTimeNoBackgroundUpdateCheck(context, e)
            Result.success() // BackgroundJob should not be removed from WorkManager schedule
        }
    }

    private fun handleUnrecoverableError(e: Exception): Result {
        BackgroundNotificationBuilder.showError(context, e)
        return Result.success() // BackgroundJob should not be removed from WorkManager schedule
    }

    @MainThread
    private suspend fun checkDownloadAndInstallApps(): Result {
        val (outdatedApps, abortCheckResult) = checkForUpdates()
        abortCheckResult?.let { result -> return result }

        val (downloadedUpdates, abortDownloadResult) = downloadUpdates(outdatedApps)
        abortDownloadResult?.let { result -> return result }

        return installUpdates(downloadedUpdates)
    }

    private suspend fun checkForUpdates(): Pair<List<App>, Result?> {
        if (!backgroundSettings.isUpdateCheckEnabled) {
            Log.i(LOG_TAG, "Background should be disabled - disable it now.")
            return listOf<App>() to Result.failure()
        }

        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return listOf<App>() to Result.retry()
        }

        if (!backgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return listOf<App>() to Result.retry()
        }

        dataStoreHelper.lastBackgroundCheck = ZonedDateTime.now()
        App.values()
            .filter { app -> app !in backgroundSettings.excludedAppsFromUpdateCheck }
            .filter { app -> app.impl.isInstalled(context) }
            .filter { app ->
                val updateResult = try {
                    app.impl.checkForUpdateWithoutLoadingFromCacheAsync(context).await()
                } catch (e: NetworkException) {
                    throw NetworkException("Fail to request latest version of ${app.name} in background.", e)
                }
                updateResult.isUpdateAvailable
            }
            .let { apps -> return apps to null }
    }

    private suspend fun downloadUpdates(apps: List<App>): Pair<List<App>, Result?> {
        if (!backgroundSettings.isDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            showUpdateNotification(apps)
            return listOf<App>() to Result.success()
        }

        if (!backgroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for download.")
            showUpdateNotification(apps)
            return listOf<App>() to Result.success()
        }

        BackgroundNotificationBuilder.hideDownloadError(context)
        val downloadedUpdates = apps.filter { downloadUpdateAndReturnAvailability(it) }
        Log.e("BackgroundJob", "these updates were downloaded: $downloadedUpdates")
        return downloadedUpdates to null
    }

    @MainThread
    private suspend fun downloadUpdateAndReturnAvailability(app: App): Boolean {
        if (!StorageUtil.isEnoughStorageAvailable(context)) {
            Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
            return false
        }

        val appCache = AppCache(app)
        val updateResult = try {
            app.impl.checkForUpdateAsync(context).await() // this result should be cached
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of ${app.name} in BackgroundJob.", e)
        }

        val availableResult = updateResult.latestUpdate
        if (appCache.isAvailable(context, availableResult)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return true
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

    private suspend fun installUpdates(apps: List<App>): Result {
        if (supportsAndroid10() && !context.packageManager.canRequestPackageInstalls()) {
            Log.i(LOG_TAG, "Missing installation permission")
            showUpdateNotification(apps)
            return Result.retry()
        }

        if (!backgroundSettings.isInstallationEnabled) {
            Log.i(LOG_TAG, "Automatic background app installation is not enabled.")
            showUpdateNotification(apps)
            return Result.success()
        }

        val installerAvailable = when {
            installerSettings.getInstaller() == ROOT_INSTALLER -> true
            supportsAndroid12() && installerSettings.getInstaller() == SESSION_INSTALLER -> true
            else -> false
        }
        if (!installerAvailable) {
            Log.i(LOG_TAG, "The current installer can not update apps in the background")
            showUpdateNotification(apps)
            return Result.success()
        }

        BackgroundNotificationBuilder.hideInstallationSuccess(context)
        BackgroundNotificationBuilder.hideInstallationError(context)
        apps.forEach { installApplication(it) }
        return Result.success()
    }

    private suspend fun installApplication(app: App) {
        val appCache = AppCache(app)
        val file = appCache.getFile(context)
        if (!file.exists()) {
            val errorMessage = "AppCache has no cached APK file"
            BackgroundNotificationBuilder.showInstallationError(context, app, -100, errorMessage)
            return
        }

        withContext(Dispatchers.Main) {
            val installer = BackgroundAppInstaller.create(context, app, file)
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

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        BackgroundNotificationBuilder.hideUpdateIsAvailable(context)
        appsWithUpdates.forEach { BackgroundNotificationBuilder.showUpdateIsAvailable(context, it) }
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"

        // first 11 delays: 15s,30s,1min,2min,4min,8min,16min,32min,64min,128min,256min = 512min
        // 12th, 13th, ... delay: 300min
        // 512min + 23*300min = 7412min = 5d 3h 32min
        private const val RUN_ATTEMPTS_FOR_MAX_5DAYS = 11 + 23

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
            interval: Duration? = null,
        ) {
            val minutes = (interval ?: settings.updateCheckInterval).toMinutes()
            val requiredNetworkType = if (settings.isUpdateCheckOnMeteredAllowed) NOT_REQUIRED else UNMETERED
            val workRequest = PeriodicWorkRequest.Builder(BackgroundJob::class.java, minutes, MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .setRequiredNetworkType(requiredNetworkType)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_MANAGER_KEY, existingPeriodicWorkPolicy, workRequest)
        }

        private fun stopBackgroundUpdateCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }
    }
}