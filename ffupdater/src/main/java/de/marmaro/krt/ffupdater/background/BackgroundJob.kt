package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.download.*
import de.marmaro.krt.ffupdater.settings.PreferencesHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.CancellationException
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
        try {
            executeBackgroundJob()
            return Result.success()
        } catch (e: Exception) {
            when (e) {
                is CancellationException, is GithubRateLimitExceededException, is NetworkException -> {
                    if (runAttemptCount <= RUN_ATTEMPTS_FOR_63MIN_TOTAL) {
                        Log.i(LOG_TAG, "Retry background job.", e)
                        return Result.retry()
                    }
                }
            }
            showErrorNotification(applicationContext, e)
            return Result.success()
        }
    }

    private fun showErrorNotification(context: Context, e: Exception) {
        val message = context.getString(R.string.background_job_failure__notification_text)
        NotificationBuilder.showErrorNotification(context, e, message)
    }

    @MainThread
    private suspend fun executeBackgroundJob(): Result {
        Log.i(LOG_TAG, "Execute background job for update check.")
        if (AppDownloadStatus.areDownloadsInForegroundActive()) {
            Log.i(LOG_TAG, "Retry background job because other downloads are running.")
            return Result.retry()
        }

        val appsWithUpdates = findAppsWithUpdates()
        if (NetworkUtil.isActiveNetworkUnmetered(applicationContext) &&
            StorageUtil.isEnoughStorageAvailable() &&
            appsWithUpdates.isNotEmpty()
        ) {
            NotificationBuilder.showDownloadNotification(applicationContext)
            appsWithUpdates.forEach {
                downloadUpdateInBackground(it)
            }
            NotificationBuilder.hideDownloadNotification(applicationContext)
        }
        showUpdateNotification(appsWithUpdates)
        updateLastBackgroundCheckTimestamp()
        return Result.success()
    }

    /**
     * Returns apps which:
     *  - are installed
     *  - are not disabled (in the settings "excluded applications")
     *  - have an available update
     */
    private suspend fun findAppsWithUpdates(): List<App> {
        val disabledApps = SettingsHelper(applicationContext).disabledApps
        return App.values()
            .filter { it !in disabledApps }
            .filter { it.detail.isInstalled(applicationContext) }
            // nice side effect: check for updates by calling updateCheck()
            .filter { it.detail.updateCheck(applicationContext).isUpdateAvailable }
    }

    /**
     * If the app update is not already been cached, then start the download and wait until the
     * download is finished.
     */
    @MainThread
    private suspend fun downloadUpdateInBackground(app: App) {
        val appCache = AppCache(app)
        val cachedUpdateChecker = app.detail.updateCheck(applicationContext)
        val availableResult = cachedUpdateChecker.availableResult
        if (appCache.isAvailable(applicationContext, availableResult)) {
            Log.i(LOG_TAG, "Skip $app download because it's already cached.")
            return
        }
        Log.i(LOG_TAG, "Download $app in the background.")

        val url = availableResult.downloadUrl
        val file = appCache.getFile(applicationContext)
        AppDownloadStatus.backgroundDownloadIsStarted()
        val result = FileDownloader().downloadFile(url, file)
        AppDownloadStatus.backgroundDownloadIsFinished()
        if (!result) {
            appCache.delete(applicationContext)
        }
    }

    private fun updateLastBackgroundCheckTimestamp() {
        PreferencesHelper(applicationContext).lastBackgroundCheck = LocalDateTime.now()
    }

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        NotificationBuilder.showUpdateNotifications(applicationContext, appsWithUpdates)
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"

        // waiting time = 0.5m + 1m + 2m + 4m + 8m + 16m + 32m = 63,5m
        private const val RUN_ATTEMPTS_FOR_63MIN_TOTAL = 7

        fun startOrStopBackgroundUpdateCheck(context: Context) {
            if (SettingsHelper(context).automaticCheck) {
                startBackgroundUpdateCheck(context)
            } else {
                stopBackgroundUpdateCheck(context)
            }
        }

        private fun startBackgroundUpdateCheck(context: Context) {
            val settingsHelper = SettingsHelper(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
            if (settingsHelper.onlyUnmeteredNetwork) {
                constraints.setRequiredNetworkType(NetworkType.UNMETERED)
            }

            val workRequest = PeriodicWorkRequest.Builder(
                BackgroundJob::class.java,
                settingsHelper.checkInterval.toMinutes(),
                MINUTES
            )
                .setConstraints(constraints.build())
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, workRequest)
        }

        private fun stopBackgroundUpdateCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }
    }
}