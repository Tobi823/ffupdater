package de.marmaro.krt.ffupdater.background

import android.app.DownloadManager
import android.content.Context
import android.util.Log
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.download.ApkCache
import de.marmaro.krt.ffupdater.download.DownloadManagerUtil
import de.marmaro.krt.ffupdater.download.DownloadManagerUtil.DownloadStatus.Status.FAILED
import de.marmaro.krt.ffupdater.download.DownloadManagerUtil.DownloadStatus.Status.SUCCESSFUL
import de.marmaro.krt.ffupdater.download.NetworkUtil
import de.marmaro.krt.ffupdater.download.StorageUtil
import de.marmaro.krt.ffupdater.settings.PreferencesHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.MINUTES

/**
 * This class will call the [WorkManager] to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 *
 * doWork can be interrupted at any time and cause a CancellationException.
 */
class BackgroundJob(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    /**
     * If:
     * - airplane mode is enabled
     * - internet is not available
     * - the system download manager is currently downloading a file
     * then delay the background job execution by 30s, 1m, 2m, 4m, ...
     * <a>https://developer.android.com/reference/androidx/work/BackoffPolicy?hl=en#EXPONENTIAL</a>
     * <a>https://developer.android.com/reference/androidx/work/WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS</a>
     *
     * Result.failure will remove the scheduled job (and that's unwanted).
     */
    override suspend fun doWork(): Result {
        val context = applicationContext
        try {
            executeBackgroundJob()
        } catch (e: CancellationException) {
            // when the network is disabled, this exception will be thrown -> ignore it
        } catch (e: ApiNetworkException) {
            // GithubRateLimitExceededException will be caught too
            val message = context.getString(R.string.background_network_issue_notification__text)
            ErrorNotificationBuilder.showNotification(context, e, message)
        } catch (e: Exception) {
            val message = context.getString(R.string.background_unknown_bug_notification__text)
            ErrorNotificationBuilder.showNotification(context, e, message)
        }
        //don't Result.retry() on exceptions to avoid error spamming
        return Result.success()
    }

    private suspend fun executeBackgroundJob(): Result {
        Log.i(LOG_TAG, "execute BackgroundJob")
        val context = applicationContext
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        if (NetworkUtil.isAirplaneModeOn(context)) {
            Log.i(LOG_TAG, "delay BackgroundJob due to enabled airplane mode")
            return Result.retry()
        }
        if (!NetworkUtil.isInternetAvailable(context)) {
            Log.i(LOG_TAG, "delay BackgroundJob because internet is not available")
            return Result.retry()
        }
        if (DownloadManagerUtil.isDownloadingAFileNow(downloadManager)) {
            Log.i(LOG_TAG, "delay BackgroundJob because other downloads are running")
            return Result.retry()
        }

        val appsWithUpdates = findAppsWithUpdates()
        if (NetworkUtil.isActiveNetworkUnmetered(context) &&
            StorageUtil.isEnoughStorageAvailable(context)) {
            downloadUpdatesInBackground(downloadManager, appsWithUpdates)
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
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     * @throws CancellationException
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
     * If the current network is unmetered, then download the update for the given apps
     * with the DownloadManager in the background.
     */
    private suspend fun downloadUpdatesInBackground(
        downloadManager: DownloadManager,
        appsWithUpdates: List<App>
    ) {
        appsWithUpdates.forEach { downloadUpdateInBackground(it, downloadManager) }
    }

    /**
     * If the app update is not already been cached, then start the download and wait until the
     * download is finished.
     */
    private suspend fun downloadUpdateInBackground(app: App, downloadManager: DownloadManager) {
        val apkCache = ApkCache(app, applicationContext)
        val cachedUpdateChecker = app.detail.updateCheck(applicationContext)
        val availableResult = cachedUpdateChecker.availableResult
        if (apkCache.isCacheAvailable(availableResult)) {
            Log.i(LOG_TAG, "skip $app download because it's already cached")
            return
        }

        Log.i(LOG_TAG, "download $app in the background")
        val downloadId = DownloadManagerUtil.enqueue(downloadManager,
            applicationContext,
            app,
            availableResult)
        repeat(60 * 60) {
            when (DownloadManagerUtil.getStatusAndProgress(downloadManager, downloadId).status) {
                SUCCESSFUL -> {
                    downloadManager.openDownloadedFile(downloadId).use { downloadedFile ->
                        apkCache.copyToCache(downloadedFile)
                    }
                    return
                }
                FAILED -> {
                    downloadManager.remove(downloadId)
                    return
                }
                else -> delay(1000)
            }
        }
        downloadManager.remove(downloadId)
    }

    private fun updateLastBackgroundCheckTimestamp() {
        PreferencesHelper(applicationContext).lastBackgroundCheck = LocalDateTime.now()
    }

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        UpdateNotificationBuilder.showNotifications(appsWithUpdates, applicationContext)
    }

    companion object {
        private const val WORK_MANAGER_KEY = "update_checker"
        private const val LOG_TAG = "BackgroundJob"

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