package de.marmaro.krt.ffupdater

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.CONNECTED
import androidx.work.NetworkType.UNMETERED
import androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
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
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationRemover
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.coroutineScope
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
    private val notificationBuilder = BackgroundNotificationBuilder.INSTANCE
    private val notificationRemover = BackgroundNotificationRemover.INSTANCE
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
        } catch (e: NetworkException) {
            handleDoWorkException(e, true)
        } catch (e: Exception) {
            handleDoWorkException(e, false)
        }
    }

    private fun handleDoWorkException(e: Exception, isNetworkException: Boolean): Result {
        return if (runAttemptCount < MAX_RETRIES) {
            Log.w(LOG_TAG, "Background job failed. Restart in ${calcBackoffTime(runAttemptCount)}", e)
            Result.retry()
        } else {
            val wrappedException = BackgroundException(e)
            Log.e(LOG_TAG, "Background job failed.", wrappedException)
            if (isNetworkException) {
                notificationBuilder.showNetworkErrorNotification(context, wrappedException)
            } else {
                notificationBuilder.showErrorNotification(context, wrappedException)
            }
            Result.success() // BackgroundJob should not be removed from WorkManager schedule
        }
    }

    @MainThread
    private suspend fun internalDoWork(): Result {
        notificationRemover.removeDownloadErrorNotification(context)
        notificationRemover.removeAppStatusNotifications(context)

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

        val outdatedApps = checkForOutdatedApps()

        if (!backgroundSettings.isDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            notificationBuilder.showUpdateAvailableNotification(context, outdatedApps)
            return Result.retry()
        }

        if (!backgroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(context)) {
            Log.i(LOG_TAG, "No unmetered network available for download.")
            notificationBuilder.showUpdateAvailableNotification(context, outdatedApps)
            return Result.retry()
        }

        val downloadingApps = outdatedApps.filter {
            if (!StorageUtil.isEnoughStorageAvailable(context)) {
                Log.i(LOG_TAG, "Skip $it because not enough storage is available.")
                notificationBuilder.showUpdateAvailableNotification(context, it)
                return@filter false
            }
            downloadUpdateAndReturnAvailability(it)
        }

        if (sdkTester.supportsAndroid10() && !context.packageManager.canRequestPackageInstalls()) {
            Log.i(LOG_TAG, "Missing installation permission")
            notificationBuilder.showUpdateAvailableNotification(context, downloadingApps)
            return Result.success()
        }

        if (!backgroundSettings.isInstallationEnabled) {
            Log.i(LOG_TAG, "Automatic background app installation is not enabled.")
            notificationBuilder.showUpdateAvailableNotification(context, downloadingApps)
            return Result.success()
        }

        val installerAvailable = when (installerSettings.getInstallerMethod()) {
            SESSION_INSTALLER -> sdkTester.supportsAndroid12()
            NATIVE_INSTALLER -> false
            ROOT_INSTALLER, SHIZUKU_INSTALLER -> true
        }
        if (!installerAvailable) {
            Log.i(LOG_TAG, "The current installer can not update apps in the background")
            notificationBuilder.showUpdateAvailableNotification(context, downloadingApps)
            return Result.success()
        }

        // update FFUpdater at last because an update will kill this update process
        val appsForInstallation = downloadingApps.toMutableList()
        if (appsForInstallation.contains(App.FFUPDATER)) {
            appsForInstallation.remove(App.FFUPDATER)
            appsForInstallation.add(App.FFUPDATER)
        }

        appsForInstallation.forEach {
            installApplication(it)
        }

        return Result.success()
    }

    private suspend fun checkForOutdatedApps(): List<App> {
        dataStoreHelper.lastBackgroundCheck = ZonedDateTime.now()
        val apps = App.values()
            .filter { it !in backgroundSettings.excludedAppsFromUpdateCheck }
            .filter { DeviceAbiExtractor.INSTANCE.supportsOneOf(it.impl.supportedAbis) }
            .filter { it.impl.isInstalled(context) == InstallationStatus.INSTALLED }
            .filter { it.metadataCache.getCachedOrFetchIfOutdated(context).isUpdateAvailable }

        apps.forEach {
            val latestUpdate = it.metadataCache.getCached(context)!!.latestUpdate
            it.downloadedFileCache.deleteAllExceptLatestApkFile(context, latestUpdate)
        }

        return apps
    }

    @MainThread
    private suspend fun downloadUpdateAndReturnAvailability(app: App): Boolean {
        val updateResult = app.metadataCache.getCachedOrFetchIfOutdated(context)
        val latestUpdate = updateResult.latestUpdate
        if (app.downloadedFileCache.isApkFileCached(context, latestUpdate)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return true
        }

        Log.i(LOG_TAG, "Download update for $app.")
        val downloader = FileDownloader(networkSettings)
        val downloadFile = app.downloadedFileCache.getApkOrZipTargetFileForDownload(context, latestUpdate)
        return try {
            // run async with await later
            val (deferred, channel) = downloader.downloadBigFileAsync(latestUpdate.downloadUrl, downloadFile)

            notificationBuilder.showDownloadRunningNotification(context, app, null, null)
            for (progress in channel) {
                notificationBuilder.showDownloadRunningNotification(
                    context, app, progress.progressInPercent, progress.totalMB
                )
            }

            deferred.await()

            if (app.impl.isAppPublishedAsZipArchive()) {
                app.downloadedFileCache.extractApkFromZipArchive(context, latestUpdate)
                app.downloadedFileCache.deleteZipFile(context)
            }

            if (latestUpdate.fileSizeBytes != null && latestUpdate.fileSizeBytes != downloadFile.length()) {
                val expected = latestUpdate.fileSizeBytes
                val actual = downloadFile.length()
                val message = "Wrong file was downloaded. It should be $expected bytes long but actual " +
                        "it was $actual bytes. FFUpdater will retry the download later."
                throw NetworkException(message)
            }

            notificationRemover.removeDownloadRunningNotification(context, app)
            app.downloadedFileCache.deleteAllExceptLatestApkFile(context, latestUpdate)
            true
        } catch (e: NetworkException) {
            notificationRemover.removeDownloadRunningNotification(context, app)
            notificationBuilder.showDownloadNotification(context, app, e)
            app.downloadedFileCache.deleteAllApkFileForThisApp(context)
            false
        }
    }

    private suspend fun installApplication(app: App) {
        val availableResult = app.metadataCache.getCachedOrExceptionIfOutdated(context).latestUpdate
        val file = app.downloadedFileCache.getApkFile(context, availableResult)
        require(file.exists()) { "AppCache has no cached APK file" }
        var success = false

        val installer = createBackgroundAppInstaller(context, app)
        try {
            installer.startInstallation(context, file)
            success = true

            notificationBuilder.showInstallSuccessNotification(context, app)
            val metadata = app.metadataCache.getCachedOrNullIfOutdated(context)
            app.impl.appIsInstalledCallback(context, metadata!!)
        } catch (e: UserInteractionIsRequiredException) {
            notificationBuilder.showUpdateAvailableNotification(context, app)
        } catch (e: InstallationFailedException) {
            val wrappedException = InstallationFailedException(
                "Failed to install ${app.name} in the background with ${installer.type}.", -532, e
            )
            notificationBuilder.showInstallFailureNotification(
                context, app, e.errorCode, e.translatedMessage, wrappedException
            )
        }

        if ((success && backgroundSettings.isDeleteUpdateIfInstallSuccessful) ||
            (!success && backgroundSettings.isDeleteUpdateIfInstallFailed)
        ) {
            app.downloadedFileCache.deleteAllApkFileForThisApp(context)
        }
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
            onlyWhenIdle: Boolean,
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
            onlyWhenIdle: Boolean,
        ) {
            val requiredNetworkType = if (settings.isUpdateCheckOnMeteredAllowed) CONNECTED else UNMETERED
            val builder = Constraints.Builder()
                .setRequiredNetworkType(requiredNetworkType)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)

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

        private fun calcBackoffTime(runAttempts: Int): Duration {
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
                totalTimeMs += calcBackoffTime(runAttempt).toMillis()
                if (totalTimeMs >= totalTime.toMillis()) {
                    return runAttempt + 1
                }
            }
            throw RuntimeException("Endless loop")
        }
    }
}