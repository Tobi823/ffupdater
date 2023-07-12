package de.marmaro.krt.ffupdater

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType.CONNECTED
import androidx.work.NetworkType.UNMETERED
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkRequest.Companion.DEFAULT_BACKOFF_DELAY_MILLIS
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppAndUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.background.BackgroundException
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createBackgroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.NATIVE_INSTALLER
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exceptions.UserInteractionIsRequiredException
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.USE_CACHE
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationRemover
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
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
@Keep
class BackgroundJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context.applicationContext, workerParams) {


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
            Log.i(LOG_TAG, "doWork(): Execute background job.")
            internalDoWork()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Log.w(LOG_TAG, "Background job failed. Restart in ${calcBackoffTime(runAttemptCount)}.", e)
                Result.retry()
            } else {
                val backgroundException = BackgroundException(e)
                Log.e(LOG_TAG, "Background job failed.", backgroundException)
                if (e is NetworkException) {
                    BackgroundNotificationBuilder.showNetworkErrorNotification(applicationContext, backgroundException)
                } else {
                    BackgroundNotificationBuilder.showErrorNotification(applicationContext, backgroundException)
                }
                Result.success() // BackgroundJob should not be removed from WorkManager schedule
            }
        }
    }

    @MainThread
    private suspend fun internalDoWork(): Result {
        BackgroundNotificationRemover.removeDownloadErrorNotification(applicationContext)
        BackgroundNotificationRemover.removeAppStatusNotifications(applicationContext)

        shouldUpdateCheckBeAborted()?.let { return it }

        val outdatedApps = checkForOutdatedApps()
        Log.d(LOG_TAG, "internalDoWork(): ${outdatedApps.map { it.app }.joinToString(",")} are outdated.")

        shouldDownloadsBeAborted()?.let {
            // execute if downloads should be aborted
            val apps = outdatedApps.map { (app, _) -> app }
            BackgroundNotificationBuilder.showUpdateAvailableNotification(applicationContext, apps)
            return it
        }

        val downloadedApps = outdatedApps.filter { (app, updateStatus) ->
            if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
                Log.i(LOG_TAG, "Skip $app because not enough storage is available.")
                BackgroundNotificationBuilder.showUpdateAvailableNotification(applicationContext, app)
                return@filter false
            }
            downloadUpdateAndReturnAvailability(app, updateStatus)
        }

        shouldInstallationBeAborted()?.let {
            // execute if installation should be aborted
            val apps = downloadedApps.map { (app, _) -> app }
            BackgroundNotificationBuilder.showUpdateAvailableNotification(applicationContext, apps)
            return it
        }

        // update FFUpdater at last because an update will kill this update process
        val appsForInstallation = downloadedApps.sortedBy { (app, _) ->
            if (app != App.FFUPDATER) app.ordinal else Int.MAX_VALUE
        }

        Log.d(LOG_TAG, "internalDoWork(): Update ${appsForInstallation.joinToString(",")}.")
        appsForInstallation.forEach { (app, updateStatus) ->
            installApplication(app, updateStatus)
        }

        return Result.success()
    }

    private fun shouldUpdateCheckBeAborted(): Result? {
        if (!BackgroundSettingsHelper.isUpdateCheckEnabled) {
            Log.i(LOG_TAG, "Background should be disabled - disable it now.")
            return Result.failure()
        }

        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return Result.retry()
        }

        if (!BackgroundSettingsHelper.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            Log.i(LOG_TAG, "No unmetered network available for update check.")
            return Result.retry()
        }

        return null
    }

    private suspend fun checkForOutdatedApps(): List<AppAndUpdateStatus> {
        DataStoreHelper.lastBackgroundCheck = ZonedDateTime.now()
        val appsAndUpdateStatus = App.values()
            // simple and fast checks
            .filter { it !in BackgroundSettingsHelper.excludedAppsFromUpdateCheck }
            .map { it.findImpl() }
            .filter { DeviceAbiExtractor.supportsOneOf(it.supportedAbis) }
            .filter { it.isInstalled(applicationContext) == InstallationStatus.INSTALLED }
            // query latest available update
            .map {
                val updateStatus = it.findAppUpdateStatus(applicationContext, USE_CACHE)
                AppAndUpdateStatus(it.app, updateStatus)
            }

        // delete old cached APK files
        appsAndUpdateStatus.forEach { (app, appUpdateStatus) ->
            app.findImpl().deleteFileCacheExceptLatest(applicationContext, appUpdateStatus.latestUpdate)
        }

        // return outdated apps with available updates
        return appsAndUpdateStatus
            .filter { (_, appUpdateStatus) -> appUpdateStatus.isUpdateAvailable }
    }

    private fun shouldDownloadsBeAborted(): Result? {
        if (!BackgroundSettingsHelper.isDownloadEnabled) {
            Log.i(LOG_TAG, "Don't download updates because the user don't want it.")
            return Result.retry()
        }

        if (!BackgroundSettingsHelper.isDownloadOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            Log.i(LOG_TAG, "No unmetered network available for download.")
            return Result.retry()
        }

        if (DeviceSdkTester.supportsAndroidNougat()) {
            val manager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (manager.restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED) {
                Log.i(LOG_TAG, "Data Saver is enabled. Do not download updates in the background.")
                return Result.retry()
            }
        }

        return null
    }

    @MainThread
    private suspend fun downloadUpdateAndReturnAvailability(app: App, updateStatus: AppUpdateStatus): Boolean {
        Log.d(LOG_TAG, "downloadUpdateAndReturnAvailability(): Download ${app.name} in background.")
        val latestUpdate = updateStatus.latestUpdate
        val appImpl = app.findImpl()
        if (appImpl.isApkDownloaded(applicationContext, latestUpdate)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return true
        }
        Log.i(LOG_TAG, "Download update for $app.")
        return try {
            appImpl.download(applicationContext, latestUpdate) { _, progressChannel ->
                BackgroundNotificationBuilder.showDownloadRunningNotification(applicationContext, app, null, null)
                for (progress in progressChannel) {
                    BackgroundNotificationBuilder.showDownloadRunningNotification(
                        applicationContext, app, progress.progressInPercent, progress.totalMB
                    )
                }
            }
            true
        } catch (e: DisplayableException) {
            BackgroundNotificationBuilder.showDownloadNotification(applicationContext, app, e)
            false
        } finally {
            BackgroundNotificationRemover.removeDownloadRunningNotification(applicationContext, app)
        }
    }

    private fun shouldInstallationBeAborted(): Result? {
        if (DeviceSdkTester.supportsAndroid10() && !applicationContext.packageManager.canRequestPackageInstalls()) {
            Log.i(LOG_TAG, "Missing installation permission")

            return Result.success()
        }

        if (!BackgroundSettingsHelper.isInstallationEnabled) {
            Log.i(LOG_TAG, "Automatic background app installation is not enabled.")
            return Result.success()
        }

        if (!DeviceSdkTester.supportsAndroid12() && InstallerSettingsHelper.getInstallerMethod() == SESSION_INSTALLER) {
            Log.i(LOG_TAG, "The current installer can not update apps in the background")
            return Result.success()
        }

        if (InstallerSettingsHelper.getInstallerMethod() == NATIVE_INSTALLER) {
            Log.i(LOG_TAG, "The current installer can not update apps in the background")
            return Result.success()
        }

        return null
    }

    private suspend fun installApplication(app: App, updateStatus: AppUpdateStatus) {
        val appImpl = app.findImpl()
        val file = appImpl.getApkFile(applicationContext, updateStatus.latestUpdate)
        require(file.exists()) { "AppCache has no cached APK file" }

        val installer = createBackgroundAppInstaller(applicationContext, app)
        try {
            installer.startInstallation(applicationContext, file)

            BackgroundNotificationBuilder.showInstallSuccessNotification(applicationContext, app)
            appImpl.installCallback(applicationContext, updateStatus)

            if (BackgroundSettingsHelper.isDeleteUpdateIfInstallSuccessful) {
                appImpl.getApkCacheFolder(applicationContext)
            }
            return
        } catch (e: UserInteractionIsRequiredException) {
            BackgroundNotificationBuilder.showUpdateAvailableNotification(applicationContext, app)
        } catch (e: InstallationFailedException) {
            val wrappedException = InstallationFailedException(
                "Failed to install ${app.name} in the background with ${installer.type}.", -532, e
            )
            BackgroundNotificationBuilder.showInstallFailureNotification(
                applicationContext, app, e.errorCode, e.translatedMessage, wrappedException
            )
        }
        if (BackgroundSettingsHelper.isDeleteUpdateIfInstallFailed) {
            appImpl.deleteFileCache(applicationContext)
        }
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private val MAX_RETRIES = getRetriesForTotalBackoffTime(Duration.ofHours(8))

        fun start(context: Context, policy: ExistingPeriodicWorkPolicy) {
            val instance = WorkManager.getInstance(context.applicationContext)
            if (!BackgroundSettingsHelper.isUpdateCheckEnabled) {
                instance.cancelUniqueWork(WORK_MANAGER_KEY)
                return
            }

            val requiredNetworkType =
                if (BackgroundSettingsHelper.isUpdateCheckOnMeteredAllowed) CONNECTED else UNMETERED
            val builder = Constraints.Builder()
                .setRequiredNetworkType(requiredNetworkType)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)

            if (DeviceSdkTester.supportsAndroidMarshmallow()) {
                builder.setRequiresDeviceIdle(BackgroundSettingsHelper.isUpdateCheckOnlyAllowedWhenDeviceIsIdle)
            }

            val minutes = BackgroundSettingsHelper.updateCheckInterval.toMinutes()
            val workRequest = PeriodicWorkRequest.Builder(BackgroundJob::class.java, minutes, MINUTES)
                .setConstraints(builder.build())
                .build()

            instance.enqueueUniquePeriodicWork(WORK_MANAGER_KEY, policy, workRequest)
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