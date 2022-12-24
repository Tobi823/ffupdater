package de.marmaro.krt.ffupdater

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.NOT_REQUIRED
import androidx.work.NetworkType.UNMETERED
import androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.background.AppsOrResult
import de.marmaro.krt.ffupdater.background.BackgroundException
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createBackgroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.*
import de.marmaro.krt.ffupdater.installer.exception.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exception.UserInteractionIsRequiredException
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import de.marmaro.krt.ffupdater.storage.StorageUtil
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
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val backgroundSettings = BackgroundSettingsHelper(preferences)
    private val installerSettings = InstallerSettingsHelper(preferences)
    private val networkSettings = NetworkSettingsHelper(preferences)
    private val dataStoreHelper = DataStoreHelper(context)
    private val notification = BackgroundNotificationBuilder.INSTANCE
    private val sdkTester = DeviceSdkTester.INSTANCE


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
            internalDoWork()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Log.w(LOG_TAG, "Background job failed.", e)
                Log.i(LOG_TAG, "Restart background job in ${calculateBackoffTime(runAttemptCount)}")
                Result.retry()
            } else {
                val wrappedException = BackgroundException(e)
                Log.e(LOG_TAG, "Background job failed.", wrappedException)
                notification.showError(context, wrappedException)
                Result.success() // BackgroundJob should not be removed from WorkManager schedule
            }
        }
    }

    @MainThread
    private suspend fun internalDoWork(): Result {
        val outdatedApps = checkForUpdates()
        if (outdatedApps.hasFailure()) {
            return outdatedApps.failure
        }

        val downloadedApps = downloadUpdates(outdatedApps.value)
        if (downloadedApps.hasFailure()) {
            return downloadedApps.failure
        }

        return installUpdates(downloadedApps.value)
    }

    private suspend fun checkForUpdates(): AppsOrResult {
        if (!backgroundSettings.isUpdateCheckEnabled) {
            Log.i(LOG_TAG, "Background should be disabled - disable it now.")
            return AppsOrResult.abortCompletely()
        }

        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return AppsOrResult.retryInIncreasingIntervals()
        }

        if (!backgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return AppsOrResult.retryInIncreasingIntervals()
        }

        dataStoreHelper.lastBackgroundCheck = ZonedDateTime.now()
        App.values()
            .filter { app -> app !in backgroundSettings.excludedAppsFromUpdateCheck }
            .filter { DeviceAbiExtractor.INSTANCE.supportsOneOf(it.impl.supportedAbis) }
            .filter { app -> app.impl.isInstalled(context) == InstallationStatus.INSTALLED }
            .filter { app ->
                val updateResult = app.metadataCache.getCachedOrFetchIfOutdated(context)
                updateResult.isUpdateAvailable
            }
            .let { apps -> return AppsOrResult.apps(apps) }
    }

    private suspend fun downloadUpdates(apps: List<App>): AppsOrResult {
        if (!backgroundSettings.isDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            showUpdateNotification(apps)
            return AppsOrResult.retryNextRegularTimeSlot()
        }

        if (!backgroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for download.")
            showUpdateNotification(apps)
            return AppsOrResult.retryNextRegularTimeSlot()
        }

        notification.hideDownloadError(context)
        val downloadedUpdates = apps.filter { downloadUpdateAndReturnAvailability(it) }
        Log.e("BackgroundJob", "these updates were downloaded: $downloadedUpdates")
        return AppsOrResult(downloadedUpdates)
    }

    @MainThread
    private suspend fun downloadUpdateAndReturnAvailability(app: App): Boolean {
        if (!StorageUtil.isEnoughStorageAvailable(context)) {
            Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
            return false
        }

        val updateResult = app.metadataCache.getCachedOrFetchIfOutdated(context)

        val availableResult = updateResult.latestUpdate
        if (app.downloadedFileCache.isLatestAppVersionCached(context, availableResult)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return true
        }

        Log.i(LOG_TAG, "Download update for $app.")
        val downloader = FileDownloader(networkSettings)
        downloader.onProgress = { progressInPercent, totalMB ->
            notification.showDownloadIsRunning(context, app, progressInPercent, totalMB)
        }
        notification.showDownloadIsRunning(context, app, null, null)

        val file = app.downloadedFileCache.getApkFile(context)
        return try {
            downloader.downloadBigFileAsync(availableResult.downloadUrl, file).await()
            notification.hideDownloadIsRunning(context, app)
            true
        } catch (e: NetworkException) {
            notification.hideDownloadIsRunning(context, app)
            notification.showDownloadError(context, app, e)
            app.downloadedFileCache.deleteApkFile(context)
            false
        }
    }

    private suspend fun installUpdates(apps: List<App>): Result {
        if (sdkTester.supportsAndroid10() && !context.packageManager.canRequestPackageInstalls()) {
            Log.i(LOG_TAG, "Missing installation permission")
            showUpdateNotification(apps)
            return Result.retry()
        }

        if (!backgroundSettings.isInstallationEnabled) {
            Log.i(LOG_TAG, "Automatic background app installation is not enabled.")
            showUpdateNotification(apps)
            return Result.success()
        }

        val installerAvailable = when (installerSettings.getInstallerMethod()) {
            SESSION_INSTALLER -> sdkTester.supportsAndroid12()
            NATIVE_INSTALLER -> false
            ROOT_INSTALLER, SHIZUKU_INSTALLER -> true
        }
        if (!installerAvailable) {
            Log.i(LOG_TAG, "The current installer can not update apps in the background")
            showUpdateNotification(apps)
            return Result.success()
        }

        notification.hideInstallationSuccess(context)
        notification.hideInstallationError(context)
        apps.forEach { installApplication(it) }
        return Result.success()
    }

    private suspend fun installApplication(app: App) {
        val file = app.downloadedFileCache.getApkFile(context)
        require(file.exists()) { "AppCache has no cached APK file" }

        withContext(Dispatchers.Main) {
            val installer = createBackgroundAppInstaller(context, app, file)
            try {
                installer.startInstallation(context)

                notification.showInstallationSuccess(context, app)
                val metadata = app.metadataCache.getCachedOrNullIfOutdated(context)
                app.impl.appIsInstalledCallback(context, metadata!!)

                if (backgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                    app.downloadedFileCache.deleteApkFile(context)
                }
            } catch (e: UserInteractionIsRequiredException) {
                notification.showUpdateIsAvailable(context, app)
                if (backgroundSettings.isDeleteUpdateIfInstallFailed) {
                    app.downloadedFileCache.deleteApkFile(context)
                }
            } catch (e: InstallationFailedException) {
                val ex = RuntimeException("Failed to install ${app.name} in the background.", e)
                notification.showInstallationError(context, app, e.errorCode, e.errorMessage, ex)
                if (backgroundSettings.isDeleteUpdateIfInstallFailed) {
                    app.downloadedFileCache.deleteApkFile(context)
                }
            }
        }
    }

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        notification.hideUpdateIsAvailable(context)
        appsWithUpdates.forEach { notification.showUpdateIsAvailable(context, it) }
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"
        private val MAX_RETRIES = getRetriesForTotalBackoffTime(Duration.ofHours(8))

        /**
         * Should be called when the user minimize the app to make sure that the background update check
         * is running.
         */
        fun initBackgroundUpdateCheck(context: Context) {
            val settings = BackgroundSettingsHelper(context)
            if (settings.isUpdateCheckEnabled) {
                start(
                    context,
                    settings,
                    KEEP,
                    settings.updateCheckInterval,
                    settings.isUpdateCheckOnlyAllowedWhenDeviceIsIdle
                )
            } else {
                stop(context)
            }
        }

        fun forceRestartBackgroundUpdateCheck(context: Context) {
            val settings = BackgroundSettingsHelper(context)
            if (settings.isUpdateCheckEnabled) {
                start(
                    context,
                    settings,
                    REPLACE,
                    settings.updateCheckInterval,
                    settings.isUpdateCheckOnlyAllowedWhenDeviceIsIdle
                )
            } else {
                stop(context)
            }
        }

        /**
         * Should be called when the user changes specific background settings.
         * If value is null, the value from SharedPreferences will be used.
         */
        fun changeBackgroundUpdateCheck(
            context: Context,
            enabled: Boolean,
            interval: Duration,
            onlyWhenIdle: Boolean
        ) {
            if (enabled) {
                start(context, BackgroundSettingsHelper(context), REPLACE, interval, onlyWhenIdle)
            } else {
                stop(context)
            }
        }

        private fun start(
            context: Context,
            settings: BackgroundSettingsHelper,
            policy: ExistingPeriodicWorkPolicy,
            interval: Duration,
            onlyWhenIdle: Boolean
        ) {
            val requiredNetworkType = if (settings.isUpdateCheckOnMeteredAllowed) NOT_REQUIRED else UNMETERED
            val builder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(requiredNetworkType)

            if (DeviceSdkTester.INSTANCE.supportsAndroidMarshmallow()) {
                builder.setRequiresDeviceIdle(onlyWhenIdle)
            }

            val minutes = interval.toMinutes()
            val workRequest = PeriodicWorkRequest.Builder(BackgroundJob::class.java, minutes, MINUTES)
                .setConstraints(builder.build())
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_MANAGER_KEY, policy, workRequest)
        }

        private fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }

        private fun calculateBackoffTime(runAttempts: Int): Duration {
            val unlimitedBackoffTime = Math.scalb(DEFAULT_BACKOFF_DELAY_MILLIS.toDouble(), runAttempts)
            val limitedBackoffTime = unlimitedBackoffTime.coerceIn(
                WorkRequest.MIN_BACKOFF_MILLIS.toDouble(),
                WorkRequest.MAX_BACKOFF_MILLIS.toDouble()
            )
            return Duration.ofMillis(limitedBackoffTime.toLong())
        }

        private fun getRetriesForTotalBackoffTime(totalTime: Duration): Int {
            var totalTimeMs = 0L
            repeat(1000) { runAttempt -> // runAttempt is zero-based
                totalTimeMs += calculateBackoffTime(runAttempt).toMillis()
                if (totalTimeMs >= totalTime.toMillis()) {
                    return runAttempt + 1
                }
            }
            throw RuntimeException("Endless loop")
        }
    }
}