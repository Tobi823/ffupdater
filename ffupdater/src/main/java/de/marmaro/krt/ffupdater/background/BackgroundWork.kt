package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
import androidx.work.ExistingPeriodicWorkPolicy.UPDATE
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MAX_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showGeneralErrorNotification
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.utils.max
import java.time.Duration
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
class BackgroundWork(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

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
        } catch (e: Exception) {
            logError("BackgroundWorker: Job failed.", e)
            showGeneralErrorNotification(applicationContext, e)
            return Result.success()
        }
    }

    @MainThread
    private suspend fun internalDoWork(): Result {
        logInfo("Execute background job.")
        DataStoreHelper.storeThatBackgroundCheckWasTrigger()
        if (!BackgroundSettings.isUpdateCheckEnabled) {
            return Result.failure()
        }

        InstalledAppsCache.updateCache(applicationContext)
        val apps = InstalledAppsCache.getAppsApplicableForBackgroundUpdate(applicationContext)
        logInfo("Enqueuing work requests for: {$apps}")
        val appWorkRequests = generateWorkRequestsForApps(apps)

        val workManager = WorkManager.getInstance(applicationContext)
        val chainer = WorkRequestChainer(workManager, DOWNLOADER_INSTALLER_KEY, REPLACE)
        val work = chainer.chainInOrder(appWorkRequests)
        work?.enqueue()

        logInfo("Done enqueuing work requests.")
        return Result.success()
    }

    private fun generateWorkRequestsForApps(apps: List<App>): List<OneTimeWorkRequest> {
        val appWorkRequests = apps.sortedBy { it.installationChronology }.map { AppUpdater.createWorkRequest(it) }
        val workFinishedListener = AppUpdaterSuccessListener.createWorkRequest()
        return appWorkRequests + workFinishedListener
    }

    companion object {
        private const val CHECK_FOR_UPDATES_KEY = "update_checker"
        private const val DOWNLOADER_INSTALLER_KEY = "ffupdater_downloader_and_installer"
        private const val CLASS_LOGTAG = "BackgroundJob:"


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
                .setInitialDelay(initialDelay.seconds, SECONDS).build()
            instance.enqueueUniquePeriodicWork(CHECK_FOR_UPDATES_KEY, policy, workRequest)
        }

        fun isBackgroundUpdateCheckReliableExecuted(): Boolean {
            if (!BackgroundSettings.isUpdateCheckEnabled) {
                return true
            }
            // if null, background job was not yet executed -> skip check
            val timeSinceExecution = DataStoreHelper.getDurationSinceBackgroundCheckWasTriggered() ?: return true
            val errorMargin = Duration.ofHours(24)
            val expectedInterval = max(BackgroundSettings.updateCheckInterval, Duration.ofMillis(MAX_BACKOFF_MILLIS))
            return timeSinceExecution < (expectedInterval + errorMargin)
        }

        private fun logInfo(message: String) {
            Log.i(LOG_TAG, "$CLASS_LOGTAG: $message")
        }

        private fun logError(@Suppress("SameParameterValue") message: String, exception: Exception) {
            Log.e(LOG_TAG, "$CLASS_LOGTAG: $message", exception)
        }
    }
}