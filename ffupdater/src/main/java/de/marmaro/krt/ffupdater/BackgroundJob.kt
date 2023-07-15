package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.pm.PackageInstaller.InstallConstraints.GENTLE_UPDATE
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkRequest.Companion.DEFAULT_BACKOFF_DELAY_MILLIS
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.background.BackgroundException
import de.marmaro.krt.ffupdater.background.BackgroundJobResult
import de.marmaro.krt.ffupdater.background.BackgroundJobResult.Companion.neverRetry
import de.marmaro.krt.ffupdater.background.BackgroundJobResult.Companion.retryRegularTimeSlot
import de.marmaro.krt.ffupdater.background.BackgroundJobResult.Companion.retrySoon
import de.marmaro.krt.ffupdater.background.BackgroundJobResult.Companion.success
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.device.PowerUtil
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createBackgroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.NATIVE_INSTALLER
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exceptions.UserInteractionIsRequiredException
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.USE_CACHE
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationRemover
import de.marmaro.krt.ffupdater.notification.NotificationBuilder
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import de.marmaro.krt.ffupdater.utils.ifTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
     *
     * Getting cancelled after 10 minutes.
     */
    @MainThread
    override suspend fun doWork(): Result {
        try {
            Log.i(LOG_TAG, "BackgroundJob: Execute background job.")
            storeBackgroundJobExecutionTime()
            areRunRequirementsMet()
                .onFailure { return it }
            val result = internalDoWork()
            Log.i(LOG_TAG, "BackgroundJob: Finish.")
            return result
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Log.w(LOG_TAG, "BackgroundJob: Job failed. Restart in ${calcBackoffTime(runAttemptCount)}.", e)
                return Result.retry()
            }

            val backgroundException = BackgroundException(e)
            Log.e(LOG_TAG, "BackgroundJob: Job failed.", backgroundException)
            when (e) {
                is NetworkException, is CancellationException ->
                    NotificationBuilder.showNetworkErrorNotification(applicationContext, backgroundException)

                else -> NotificationBuilder.showErrorNotification(applicationContext, backgroundException)
            }
            return Result.success() // BackgroundJob should not be removed from WorkManager schedule
        }
    }

    private fun storeBackgroundJobExecutionTime() {
        DataStoreHelper.lastBackgroundCheck2 = System.currentTimeMillis()
    }

    private fun areRunRequirementsMet(): BackgroundJobResult {
        if (PowerUtil.isBatteryLow()) {
            return retryRegularTimeSlot("BackgroundJob: Skip because battery is low.")
        }
        if (NetworkUtil.isDataSaverEnabled(applicationContext)) {
            return retrySoon("BackgroundJob: Skip due to data saver.")
        }
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            return retrySoon("BackgroundJob: Skip because network is metered.")
        }
        if (BackgroundSettings.isUpdateCheckOnlyAllowedWhenDeviceIsIdle && PowerUtil.isDeviceInteractive()) {
            return retrySoon("BackgroundJob: Skip because device is not idle.")
        }
        return success()
    }

    @MainThread
    private suspend fun internalDoWork(): Result {
        BackgroundNotificationRemover.removeDownloadErrorNotification(applicationContext)
        BackgroundNotificationRemover.removeAppStatusNotifications(applicationContext)

        checkUpdateCheckAllowed().onFailure {
            return@internalDoWork it
        }
        val outdatedApps = findForOutdatedApps()

        checkDownloadsAllowed().onFailure {
            val apps = outdatedApps.map { app -> app.app }
            NotificationBuilder.showUpdateAvailableNotification(applicationContext, apps)
            return@internalDoWork it
        }
        filterAppsForDownload(outdatedApps)
            .forEach { downloadApp(it.app, it.latestVersion) }

        shouldInstallationBeAborted().onFailure {
            val apps = outdatedApps.map { app -> app.app }
            NotificationBuilder.showUpdateAvailableNotification(applicationContext, apps)
            return@internalDoWork it
        }
        filterAppsForInstallation(outdatedApps)
            .forEach { installApplication(it) }

        return Result.success()
    }

    private fun checkUpdateCheckAllowed(): BackgroundJobResult {
        return when {
            !BackgroundSettings.isUpdateCheckEnabled ->
                neverRetry("Background should be disabled - disable it now.")

            FileDownloader.areDownloadsCurrentlyRunning() ->
                retrySoon("Retry background job because other downloads are running.")

            !BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext) ->
                retrySoon("No unmetered network available for update check.")

            else -> success()
        }
    }

    private suspend fun findForOutdatedApps(): List<InstalledAppStatus> {
        DataStoreHelper.lastBackgroundCheck = ZonedDateTime.now()
        InstalledAppsCache.updateCache(applicationContext)
        val installedAppStatusList = InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(applicationContext)
            .filter { it !in BackgroundSettings.excludedAppsFromUpdateCheck }
            .filter { DeviceAbiExtractor.supportsOneOf(it.findImpl().supportedAbis) }
            // query latest available update
            .map { it.findImpl().findInstalledAppStatus(applicationContext, USE_CACHE) }

        // delete old cached APK files
        installedAppStatusList.forEach {
            it.app.findImpl().deleteFileCacheExceptLatest(applicationContext, it.latestVersion)
        }

        val outdatedApps = installedAppStatusList
            .filter { it.isUpdateAvailable }

        Log.d(LOG_TAG, "BackgroundJob: [${outdatedApps.map { it.app }.joinToString(",")}] are outdated.")
        return outdatedApps
    }

    private fun checkDownloadsAllowed(): BackgroundJobResult {
        return when {
            !BackgroundSettings.isDownloadEnabled ->
                retrySoon("Don't download updates because the user don't want it.")

            !BackgroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(applicationContext) ->
                retrySoon("No unmetered network available for download.")

            NetworkUtil.isDataSaverEnabled(applicationContext) ->
                retrySoon("Abort because data saver is enabled.")

            else -> success()
        }
    }

    private fun filterAppsForDownload(apps: List<InstalledAppStatus>): List<InstalledAppStatus> {
        return apps
            .filter {
                val enoughStorage = StorageUtil.isEnoughStorageAvailable(applicationContext)
                if (!enoughStorage) {
                    Log.i(LOG_TAG, "BackgroundJob: Skip ${it.app} because not enough storage is available.")
                    NotificationBuilder.showUpdateAvailableNotification(applicationContext, it.app)
                }
                enoughStorage
            }
            .filter { !it.app.findImpl().isApkDownloaded(applicationContext, it.latestVersion) }
    }

    @MainThread
    private suspend fun downloadApp(app: App, latestVersion: LatestVersion) {
        val appImpl = app.findImpl()
        Log.i(LOG_TAG, "BackgroundJob: Download update for $app.")
        try {
            appImpl.download(applicationContext, latestVersion) { _, progressChannel ->
                NotificationBuilder.showDownloadRunningNotification(applicationContext, app, null, null)
                var lastTime = System.currentTimeMillis()
                for (progress in progressChannel) {
                    val time = System.currentTimeMillis()
                    if ((time - lastTime) > 1000) {
                        lastTime = time
                        NotificationBuilder.showDownloadRunningNotification(
                            applicationContext, app, progress.progressInPercent, progress.totalMB
                        )
                    }
                }
            }
        } catch (e: DisplayableException) {
            NotificationBuilder.showDownloadNotification(applicationContext, app, e)
        } finally {
            BackgroundNotificationRemover.removeDownloadRunningNotification(applicationContext, app)
        }
    }

    private fun shouldInstallationBeAborted(): BackgroundJobResult {
        return when {
            DeviceSdkTester.supportsAndroid10() && !applicationContext.packageManager.canRequestPackageInstalls() ->
                retryRegularTimeSlot("BackgroundJob: Missing installation permission.")

            !BackgroundSettings.isInstallationEnabled ->
                retryRegularTimeSlot("Automatic background app installation is not enabled.")

            !DeviceSdkTester.supportsAndroid12() && InstallerSettings.getInstallerMethod() == SESSION_INSTALLER ->
                retryRegularTimeSlot("The current installer can not update apps in the background")

            InstallerSettings.getInstallerMethod() == NATIVE_INSTALLER ->
                retryRegularTimeSlot("The current installer can not update apps in the background")

            else -> success()
        }
    }

    private suspend fun filterAppsForInstallation(apps: List<InstalledAppStatus>): List<InstalledAppStatus> {
        return apps
            // update FFUpdater at last because an update will kill this update process
            .sortedBy { if (it.app != App.FFUPDATER) it.app.ordinal else Int.MAX_VALUE }
            .filter { it.app.findImpl().isApkDownloaded(applicationContext, it.latestVersion) }
            .filter { shouldUpdateBeInstalled(it.app) }
    }

    private suspend fun shouldUpdateBeInstalled(app: App): Boolean {
        if (DeviceSdkTester.supportsAndroid14()) {
            val gentleUpdatePossible = CompletableDeferred<Boolean>()
            val value = try {
                val installer = applicationContext.packageManager.packageInstaller
                val apps = listOf(app.findImpl().packageName)
                val executor = applicationContext.mainExecutor
                installer.checkInstallConstraints(apps, GENTLE_UPDATE, executor) {
                    val gentle = it.areAllConstraintsSatisfied()
                    gentleUpdatePossible.complete(gentle)
                }
                gentleUpdatePossible.await()
            } catch (e: SecurityException) {
                Log.i(LOG_TAG, "BackgroundJob: Can't check if $app can be gently updated.")
                gentleUpdatePossible.complete(true)
            }
            if (!value) {
                Log.i(LOG_TAG, "BackgroundJob: Skip $app because it is still in use.")
            }
            return value
        }

        if (BackgroundSettings.isInstallationWhenScreenOff) {
            return PowerUtil.isDeviceInteractive()
                .ifTrue { Log.i(LOG_TAG, "internalDoWork(): Skip $app because the device is still interactive.") }
        }
        return true
    }

    private suspend fun installApplication(installedAppStatus: InstalledAppStatus) {
        val app = installedAppStatus.app
        val appImpl = app.findImpl()
        val file = appImpl.getApkFile(applicationContext, installedAppStatus.latestVersion)

        val installer = createBackgroundAppInstaller(applicationContext, app)
        try {
            Log.i(LOG_TAG, "BackgroundJob: Update $app.")
            installer.startInstallation(applicationContext, file)

            NotificationBuilder.showInstallSuccessNotification(applicationContext, app)
            appImpl.appWasInstalledCallback(applicationContext, installedAppStatus)

            if (BackgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                appImpl.getApkCacheFolder(applicationContext)
            }
            return
        } catch (e: UserInteractionIsRequiredException) {
            NotificationBuilder.showUpdateAvailableNotification(applicationContext, app)
        } catch (e: InstallationFailedException) {
            val wrappedException = InstallationFailedException(
                "Failed to install ${app.name} in the background with ${installer.type}.", -532, e
            )
            NotificationBuilder.showInstallFailureNotification(
                applicationContext, app, e.errorCode, e.translatedMessage, wrappedException
            )
        }
        if (BackgroundSettings.isDeleteUpdateIfInstallFailed) {
            appImpl.deleteFileCache(applicationContext)
        }
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private val MAX_RETRIES = getRetriesForTotalBackoffTime(Duration.ofHours(8))

        fun start(context: Context, policy: ExistingPeriodicWorkPolicy) {
            val instance = WorkManager.getInstance(context.applicationContext)
            if (!BackgroundSettings.isUpdateCheckEnabled) {
                instance.cancelUniqueWork(WORK_MANAGER_KEY)
                return
            }

            val minutes = BackgroundSettings.updateCheckInterval.toMinutes()
            val workRequest = PeriodicWorkRequest.Builder(BackgroundJob::class.java, minutes, MINUTES).build()
            instance.enqueueUniquePeriodicWork(WORK_MANAGER_KEY, policy, workRequest)
        }

        fun wasBackgroundJobRegularlyExecutedAsExpected(): Boolean {
            if (!BackgroundSettings.isUpdateCheckEnabled) {
                return true
            }
            val lastExecutionTime = DataStoreHelper.lastBackgroundCheck2
            // background job was not yet executed -> skip check
            if (lastExecutionTime == 0L) {
                return true
            }

            val intervalSettings = BackgroundSettings.updateCheckInterval
            val maxIntervalRetries = Duration.ofHours(5)
            val interval = if (intervalSettings > maxIntervalRetries) intervalSettings else maxIntervalRetries
            val intervalWithErrorMargin = interval + Duration.ofHours(24)

            val timeSinceExecution = Duration.ofMillis(System.currentTimeMillis() - lastExecutionTime)
            return timeSinceExecution > intervalWithErrorMargin
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