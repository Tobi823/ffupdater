package de.marmaro.krt.ffupdater.background

import android.content.Context
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter.DownloadStatus.Status.FAILED
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter.DownloadStatus.Status.SUCCESSFUL
import de.marmaro.krt.ffupdater.download.DownloadedApkCache
import de.marmaro.krt.ffupdater.download.NetworkTester
import de.marmaro.krt.ffupdater.download.StorageTester
import de.marmaro.krt.ffupdater.settings.PreferencesHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.coroutines.cancellation.CancellationException

/**
 * This class will call the [WorkManager] to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 */
class BackgroundJob(
        context: Context,
        workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    val deviceEnvironment = DeviceEnvironment()

    override suspend fun doWork(): Result {
        return try {
            waitForInternet()
            val appsWithUpdates = findAppsWithUpdates()
            waitForUnmeteredNetwork()
            downloadUpdatesInBackground(appsWithUpdates)
            showUpdateNotification(appsWithUpdates)
            PreferencesHelper(applicationContext).lastBackgroundCheck = LocalDateTime.now()
            Result.success()
        } catch (e: BackgroundNetworkException) {
            val message = applicationContext.getString(R.string.background_network_issue_notification__text)
            ErrorNotificationBuilder.showNotification(applicationContext, e, message)
            Result.success()
        } catch (e: Exception) {
            val message = applicationContext.getString(R.string.background_unknown_bug_notification__text)
            ErrorNotificationBuilder.showNotification(applicationContext, e, message)
            Result.success()
        }
    }

    /**
     * Wait until Internet is available. Abort after 60 seconds.
     */
    private suspend fun waitForInternet() {
        repeat(60) {
            if (!NetworkTester.isInternetUnavailable(applicationContext)) {
                return
            }
            delay(1000)
        }
    }

    /**
     * Wait until the current network is unmetered. Abort after 60 seconds.
     * This is necessary because the background download will only work on unmetered networks.
     * If internet becomes unavailable, abort.
     */
    private suspend fun waitForUnmeteredNetwork() {
        repeat(60) {
            if (NetworkTester.isActiveNetworkUnmetered(applicationContext)) {
                return
            }
            if (NetworkTester.isInternetUnavailable(applicationContext)) {
                return
            }
            delay(1000)
        }
    }

    /**
     * Returns apps which:
     *  - are installed
     *  - are not disabled (in the settings "excluded applications")
     *  - have an available update
     */
    private suspend fun findAppsWithUpdates(): List<App> {
        val disabledApps = SettingsHelper(applicationContext).disabledApps
        val appsWithUpdatesAvailable = App.values()
                .filter { !disabledApps.contains(it) }
                .filter { it.detail.isInstalled(applicationContext) }
        return appsWithUpdatesAvailable.filter {
            try {
                it.detail.updateCheck(applicationContext, deviceEnvironment).isUpdateAvailable
            } catch (e: ApiConsumer.ApiConsumerRetryIOException) {
                throw BackgroundNetworkException("fail to check $it due to network error", e)
            } catch (e: CancellationException) {
                throw BackgroundNetworkException("fail to check $it due to cancelled job", e)
            } catch (e: Exception) {
                throw BackgroundUnknownException("fail to check $it due to unknown bug", e)
            }
        }
    }

    /**
     * If the current network is unmetered, then download the update for the given apps
     * with the DownloadManager in the background.
     */
    private suspend fun downloadUpdatesInBackground(appsWithUpdates: List<App>) {
        if (NetworkTester.isActiveNetworkUnmetered(applicationContext)) {
            val downloadManager = DownloadManagerAdapter.create(applicationContext)
            appsWithUpdates.forEach { downloadUpdateInBackground(it, downloadManager) }
        }
    }

    /**
     * Start the download of an app update and wait until the download is finished.
     * Preconditions for the background download:
     *  - enough memory
     *  - update must not be already downloaded
     */
    private suspend fun downloadUpdateInBackground(app: App, downloadManager: DownloadManagerAdapter) {
        val apkCache = DownloadedApkCache(app, applicationContext)
        val cachedUpdateChecker = app.detail.updateCheck(applicationContext, deviceEnvironment)
        val availableResult = cachedUpdateChecker.availableResult
        if (apkCache.isCacheAvailable(availableResult) ||
                !StorageTester.isEnoughStorageAvailable(applicationContext)) {
            return
        }

        val fileReservation = downloadManager.reserveFile(app, applicationContext)
        val downloadId = downloadManager.enqueue(applicationContext, app, availableResult, fileReservation)
        repeat(5 * 60) {
            when (downloadManager.getStatusAndProgress(downloadId).status) {
                SUCCESSFUL -> {
                    apkCache.copyFileToCache(fileReservation.downloadLocation)
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

    private fun showUpdateNotification(appsWithUpdates: List<App>) {
        UpdateNotificationBuilder.showNotifications(appsWithUpdates, applicationContext)
    }

    companion object {
        private const val WORK_MANAGER_KEY: String = "update_checker"

        fun startOrStopBackgroundUpdateCheck(context: Context) {
            if (SettingsHelper(context).automaticCheck) {
                startBackgroundUpdateCheck(context)
            } else {
                stopBackgroundUpdateCheck(context)
            }
        }

        private fun startBackgroundUpdateCheck(context: Context) {
            val settingsHelper = SettingsHelper(context)
            val repeatInterval = settingsHelper.checkInterval
            val onlyUnmetered = settingsHelper.onlyUnmeteredNetwork

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .also { if (onlyUnmetered) it.setRequiredNetworkType(NetworkType.UNMETERED) }
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            val saveRequest = PeriodicWorkRequest.Builder(
                    BackgroundJob::class.java, repeatInterval.toMinutes(), MINUTES)
                    .setConstraints(constraints)
                    .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest)
        }

        private fun stopBackgroundUpdateCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }
    }

    class BackgroundNetworkException(message: String, throwable: Throwable) :
            Exception(message, throwable)

    class BackgroundUnknownException(message: String, throwable: Throwable) :
            Exception(message, throwable)
}