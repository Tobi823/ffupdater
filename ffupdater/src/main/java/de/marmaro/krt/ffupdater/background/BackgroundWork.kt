package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
import androidx.work.ExistingPeriodicWorkPolicy.UPDATE
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.device.PowerSaveModeReceiver
import de.marmaro.krt.ffupdater.device.PowerUtil
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showGeneralErrorNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showNetworkErrorNotification
import de.marmaro.krt.ffupdater.notification.NotificationRemover
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.utils.WorkManagerTiming.calcBackoffTime
import de.marmaro.krt.ffupdater.utils.WorkManagerTiming.getRetriesForTotalBackoffTime
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

/**
 * BackgroundJob will be regularly called by the AndroidX WorkManager to:
 * - check for app updates
 * - download them
 * - install them
 *
 * Depending on the device and the settings from the user not all steps will be executed.
 */
@Keep
class BackgroundWork(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context.applicationContext,
        workerParams) {

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
            return internalDoWork()
        } catch (e: CancellationException) {
            // retry next regular time slot
            return Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                logWarn("Job failed. Restart in ${calcBackoffTime(runAttemptCount)}.", e)
                // retry as soon as possible (with backoff time)
                return Result.retry()
            }

            val backgroundException = BackgroundException(e)
            logError("BackgroundWorker: Job failed.", backgroundException)
            when (e) {
                is NetworkException -> showNetworkErrorNotification(applicationContext, backgroundException)
                else -> showGeneralErrorNotification(applicationContext, backgroundException)
            }
            // retry next regular time slot
            return Result.success()
        }
    }

    private fun storeBackgroundJobExecutionTime() {
        DataStoreHelper.lastBackgroundCheck2 = System.currentTimeMillis()
    }


    @MainThread
    private suspend fun internalDoWork(): Result {
        Log.i(LOG_TAG, "BackgroundWork: Execute background job.")
        storeBackgroundJobExecutionTime()

        if (shouldRetryNextTimeSlot()) {
            return Result.success()
        }
        if (shouldFastRetry()) {
            return Result.retry()
        }
        if (shouldNeverRetry()) {
            return Result.failure()
        }

        NotificationRemover.removeDownloadErrorNotification(applicationContext)
        NotificationRemover.removeAppStatusNotifications(applicationContext)

        val workRequests = findOutdatedApps().sortedBy { it.installationChronology }
            .map { AppUpdater.createWorkRequest(it) }

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.beginUniqueWork(DOWNLOADER_INSTALLER_KEY, KEEP, workRequests)
            .enqueue()

        logInfo("Finish.")
        return Result.success()
    }

    private fun shouldNeverRetry(): Boolean {
        if (!BackgroundSettings.isUpdateCheckEnabled) {
            logInfo("Background should be disabled - disable it now.")
            return true
        }
        return false
    }

    private fun shouldRetryNextTimeSlot(): Boolean {
        if (PowerUtil.isBatteryLow()) {
            logInfo("Skip because battery is low.")
            return true
        }
        if (PowerSaveModeReceiver.isPowerSaveModeEnabledForShortTime()) {
            logInfo("Skip because power save mode was enabled recently")
            return true
        }
        return false
    }

    private fun shouldFastRetry(): Boolean {
        if (NetworkUtil.isDataSaverEnabled(applicationContext)) {
            logInfo("Skip due to data saver.")
            return true
        }
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            logInfo("Skip because network is metered.")
            return true
        }
        if (BackgroundSettings.isUpdateCheckOnlyAllowedWhenDeviceIsIdle && PowerUtil.isDeviceInteractive()) {
            logInfo("Skip because device is not idle.")
            return true
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            logInfo("Retry background job because other downloads are running.")
            return true
        }
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            logInfo("No unmetered network available for update check.")
            return true
        }
        return false
    }


    @Suppress("ConvertCallChainIntoSequence")
    private suspend fun findOutdatedApps(): List<App> {
        InstalledAppsCache.updateCache(applicationContext)
        val appsToCheck = InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(applicationContext)
            .filter { it !in BackgroundSettings.excludedAppsFromUpdateCheck }
            .filter { it !in ForegroundSettings.hiddenApps }
            .filter { DeviceAbiExtractor.supportsOneOf(it.findImpl().supportedAbis) }
            .map { it.findImpl() }
            .filter { !it.wasInstalledByOtherApp(applicationContext) }
            .filter { FileDownloader.isUrlAvailable(it.hostnameForInternetCheck) }
            .map { it.findStatusOrUseRecentCache(applicationContext) }

        val outdatedApps = appsToCheck.filter { it.isUpdateAvailable }
            .map { it.app }

        // delete old cached APK files
        appsToCheck.forEach {
            it.app.findImpl()
                .deleteFileCacheExceptLatest(applicationContext, it.latestVersion)
        }

        logInfo("The apps ${outdatedApps.joinToString(",")} are outdated.")
        return outdatedApps
    }

    companion object {
        private const val CHECK_FOR_UPDATES_KEY = "update_checker"
        private const val DOWNLOADER_INSTALLER_KEY = "ffupdater_downloader_and_installer"
        private const val CLASS_LOGTAG = "BackgroundJob:"
        private val MAX_RETRIES = getRetriesForTotalBackoffTime(Duration.ofHours(8))

        fun start(context: Context) {
            logInfo("Start BackgroundWork")
            internalStart(context.applicationContext, UPDATE)
        }

        fun forceRestart(context: Context) {
            logInfo("Force restart BackgroundWork")
            internalStart(context.applicationContext, CANCEL_AND_REENQUEUE)
        }

        private fun internalStart(
            context: Context,
            policy: ExistingPeriodicWorkPolicy,
            initialDelay: Duration = Duration.ZERO,
        ) {
            val instance = WorkManager.getInstance(context.applicationContext)
            if (!BackgroundSettings.isUpdateCheckEnabled) {
                instance.cancelUniqueWork(CHECK_FOR_UPDATES_KEY)
                return
            }

            val minutes = BackgroundSettings.updateCheckInterval.toMinutes()
            val workRequest = PeriodicWorkRequest.Builder(BackgroundWork::class.java, minutes, MINUTES)
                .setInitialDelay(initialDelay.seconds, SECONDS)
                .build()
            instance.enqueueUniquePeriodicWork(CHECK_FOR_UPDATES_KEY, policy, workRequest)
        }

        fun isBackgroundUpdateCheckReliableExecuted(): Boolean {
            if (!BackgroundSettings.isUpdateCheckEnabled) {
                return true
            }
            val lastExecutionTime = DataStoreHelper.lastBackgroundCheck2
            // background job was not yet executed -> skip check
            if (lastExecutionTime == 0L) {
                return true
            }

            val intervalSettings = BackgroundSettings.updateCheckInterval
            val maxRetryInterval = Duration.ofHours(5)
            val interval = if (intervalSettings > maxRetryInterval) intervalSettings else maxRetryInterval
            val intervalWithErrorMargin = interval + Duration.ofHours(24)

            val timeSinceExecution = Duration.ofMillis(System.currentTimeMillis() - lastExecutionTime)
            return timeSinceExecution < intervalWithErrorMargin
        }

        private fun logInfo(message: String) {
            Log.i(LOG_TAG, "$CLASS_LOGTAG: $message")
        }

        private fun logWarn(message: String) {
            Log.w(LOG_TAG, "$CLASS_LOGTAG: $message")
        }

        private fun logWarn(message: String, exception: Exception) {
            Log.w(LOG_TAG, "$CLASS_LOGTAG: $message", exception)
        }

        private fun logError(message: String) {
            Log.e(LOG_TAG, "$CLASS_LOGTAG: $message")
        }

        private fun logError(message: String, exception: Exception) {
            Log.e(LOG_TAG, "$CLASS_LOGTAG: $message", exception)
        }
    }
}