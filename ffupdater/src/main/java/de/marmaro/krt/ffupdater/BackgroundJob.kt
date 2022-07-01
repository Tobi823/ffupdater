package de.marmaro.krt.ffupdater

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.NetworkType.UNMETERED
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.device.DeviceSdkTester.supportsAndroid10
import de.marmaro.krt.ffupdater.device.DeviceSdkTester.supportsAndroid12
import de.marmaro.krt.ffupdater.installer.BackgroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.ROOT_INSTALLER
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.GithubRateLimitExceededException
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
import java.time.LocalDateTime
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
     * NetworkException) occurs, then retry in a short time.
     * The wait time between every run is logarithmic with a max value:
     * 15s,30s,1m,2m,4m,8m,16m,32m,64m,128m,256m,300m,300m,...
     *
     * If an uncommon exception (anything else) occurs, then retry in the next regular execution interval.
     *
     * I can't return Result.error() because even if an unknown exception occurs, I want that BackgroundJob
     * is still regularly executed.
     * But Result.error() will remove BackgroundJob from the WorkManager job schedule.
     */
    @MainThread
    override suspend fun doWork(): Result = coroutineScope {
        val (exception, maxRetries) = try {
            Log.i(LOG_TAG, "Execute background job for update check.")
            return@coroutineScope checkDownloadAndInstallApps()
        } catch (e: CancellationException) {
            Log.w(LOG_TAG, "Background job failed (CancellationException)", e)
            e to RUN_ATTEMPTS_FOR_MAX_1HOUR
        } catch (e: GithubRateLimitExceededException) {
            Log.w(LOG_TAG, "Background job failed (GithubRateLimitExceededException)", e)
            e to RUN_ATTEMPTS_FOR_MAX_2DAYS
        } catch (e: NetworkException) {
            Log.w(LOG_TAG, "Background job failed (NetworkException)", e)
            e to RUN_ATTEMPTS_FOR_MAX_2DAYS
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Background job failed due to an unexpected exception", e)
            e to 0
        }

        if (runAttemptCount < maxRetries) {
            Log.i(LOG_TAG, "Retry background job.", exception)
            Result.retry()
        } else {
            showErrorNotification(exception)
            Result.success() // BackgroundJob should not be removed from WorkManager schedule
        }
    }

    @MainThread
    private suspend fun checkDownloadAndInstallApps(): Result {
        val (outdatedApps, abortCheckResult) = checkForUpdates()
        abortCheckResult?.let { result -> return result }

        val (downloadedUpdates, abortDownloadResult) = downloadUpdates(outdatedApps)
        abortDownloadResult?.let { result -> return result }

        return installUpdates(downloadedUpdates)
    }

    private suspend fun checkForUpdates(): Pair<List<MaintainedApp>, Result?> {
        if (!backgroundSettings.isUpdateCheckEnabled) {
            Log.i(LOG_TAG, "Background should be disabled - disable it now.")
            return listOf<MaintainedApp>() to Result.failure()
        }

        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return listOf<MaintainedApp>() to Result.retry()
        }

        if (!backgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return listOf<MaintainedApp>() to Result.retry()
        }

        dataStoreHelper.lastBackgroundCheck = LocalDateTime.now()
        MaintainedApp.values()
            .filter { app -> app !in backgroundSettings.excludedAppsFromUpdateCheck }
            .filter { app -> app.detail.isInstalled(context) }
            .filter { app -> app.detail.checkForUpdateWithoutCacheAsync(context).await().isUpdateAvailable }
            .let { apps -> return apps to null }
    }

    private suspend fun downloadUpdates(apps: List<MaintainedApp>): Pair<List<MaintainedApp>, Result?> {
        if (!backgroundSettings.isDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            showUpdateNotification(apps)
            return listOf<MaintainedApp>() to Result.success()
        }

        if (!backgroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for download.")
            showUpdateNotification(apps)
            return listOf<MaintainedApp>() to Result.success()
        }

        BackgroundNotificationBuilder.hideDownloadError(context)
        val downloadedUpdates = apps.filter { downloadUpdateAndReturnAvailability(it) }
        Log.e("BackgroundJob", "these updates were downloaded: $downloadedUpdates")
        return downloadedUpdates to null
    }

    @MainThread
    private suspend fun downloadUpdateAndReturnAvailability(app: MaintainedApp): Boolean {
        if (!StorageUtil.isEnoughStorageAvailable(context)) {
            Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
            return false
        }

        val appCache = AppCache(app)
        val updateResult = app.detail.checkForUpdateAsync(context).await() // this result should be cached
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

    private suspend fun installUpdates(apps: List<MaintainedApp>): Result {
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

    private suspend fun installApplication(app: MaintainedApp) {
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

    private fun showUpdateNotification(appsWithUpdates: List<MaintainedApp>) {
        BackgroundNotificationBuilder.hideUpdateIsAvailable(context)
        appsWithUpdates.forEach { BackgroundNotificationBuilder.showUpdateIsAvailable(context, it) }
    }

    private fun showErrorNotification(e: Exception) {
        val message = context.getString(R.string.background_notification__text)
        BackgroundNotificationBuilder.showError(context, e, message)
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"

        // retry delays: 15s,30s,1m,2m,4m,8m,16m,32m
        private const val RUN_ATTEMPTS_FOR_MAX_1HOUR = 8

        // retry delays: 15s,30s,1m,2m,4m,8m,16m,32m,64m,128m,256m,300m,300m,300m,300m,300m,300m,300m,300m
        private const val RUN_ATTEMPTS_FOR_MAX_2DAYS = 19

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