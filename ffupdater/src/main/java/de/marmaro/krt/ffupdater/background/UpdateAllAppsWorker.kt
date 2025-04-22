package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.background.exception.AppUpdaterNonRetryableException
import de.marmaro.krt.ffupdater.background.exception.AppUpdaterRetryableException
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.NotificationBuilder
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showDownloadRunningNotification
import de.marmaro.krt.ffupdater.notification.NotificationRemover.removeDownloadRunningNotification
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.coroutines.cancellation.CancellationException

@Keep
class UpdateAllAppsWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        DataStoreHelper.storeThatBackgroundCheckWasTrigger()
        InstalledAppsCache.updateCache(applicationContext)
        val apps = InstalledAppsCache.getAppsApplicableForBackgroundUpdate(applicationContext)
        logInfo("Update check and download updates for: {$apps}")

        try {
            NotificationBuilder.UpdateAllUpdates.showStartNotification(applicationContext)
            for (app in apps) {
                updateCheckWrapper(app).map { downloadWrapper(it) }
            }
            NotificationBuilder.UpdateAllUpdates.showFinishNotification(applicationContext)
        } catch (e: CancellationException) {
            throw e // CancellationException is normal and should not treat as error
        } catch (e: Exception) {
            NotificationBuilder.UpdateAllUpdates.showErrorNotification(applicationContext, e) //TODO ignore some errors
            throw e
        } finally {
            NotificationBuilder.UpdateAllUpdates.hideStartNotification(applicationContext)

        }

        logInfo("Done enqueuing work requests.")
        return Result.success()
    }

    private suspend fun updateCheckWrapper(app: App): kotlin.Result<InstalledAppStatus> {
        logInfo("Start for ${app.name}.")
        isUpdateCheckPossible(app).onFailure { return failure(it) }
        val installedAppStatus = try {
            app.findImpl().findStatusOrUseRecentCache(applicationContext)
        } catch (e: CancellationException) {
            throw e // CancellationException is normal and should not treat as error
        } catch (e: Exception) {
            return failure(e)
        }
        if (!installedAppStatus.isUpdateAvailable) {
            return failure(AppUpdaterNonRetryableException("No update available for ${app.name}."))
        }
        return success(installedAppStatus)
    }

    private suspend fun isUpdateCheckPossible(app: App): kotlin.Result<Boolean> {
        if (isStopped) {
            return failure(AppUpdaterNonRetryableException("WorkRequest is stopped."))
        }
        if (!FileDownloader.isUrlAvailable(app.findImpl().hostnameForInternetCheck)) {
            return failure(AppUpdaterRetryableException("Simple network test was not successful. Retry later."))
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            return failure(AppUpdaterRetryableException("Other downloads are running. Retry later."))
        }
        return success(true)
    }

    private suspend fun downloadWrapper(installedAppStatus: InstalledAppStatus): kotlin.Result<Boolean> {
        installedAppStatus.app.findImpl()
            .deleteFileCacheExceptLatest(applicationContext, installedAppStatus.latestVersion)
        isDownloadPossible(installedAppStatus.app).onFailure { return failure(it) }
        val app = installedAppStatus.app
        if (app.findImpl().isApkDownloaded(applicationContext, installedAppStatus.latestVersion)) {
            logInfo("File for ${app.name} is already downloaded")
            return success(true)
        }
        isDownloadPossible(installedAppStatus.app).onFailure { return failure(it) }

        logInfo("Start downloading ${app.name}")
        showDownloadRunningNotification(applicationContext, app, null, null)
        val result = download(installedAppStatus) {
            withContext(Dispatchers.Main) {
                showDownloadRunningNotification(applicationContext, app, it.progressInPercent, it.totalMB)
            }
        }
        removeDownloadRunningNotification(applicationContext, app)
        return result
    }

    private suspend fun download(
        installedAppStatus: InstalledAppStatus, onUpdate: suspend (DownloadStatus) -> Unit
    ): kotlin.Result<Boolean> {
        val appImpl = installedAppStatus.app.findImpl()
        try {
            coroutineScope {
                logInfo("Download update for ${installedAppStatus.app}.")
                val progress = Channel<DownloadStatus>()
                val download = async { // capture exception so that CrashListener will not caught it
                    try {
                        appImpl.download(applicationContext, installedAppStatus.latestVersion, progress)
                        return@async success(true)
                    } catch (e: CancellationException) {
                        throw e // CancellationException is normal and should not treat as error
                    } catch (e: Exception) {
                        return@async failure(e)
                    } finally {
                        progress.close(DisplayableException("Progress channel was not yet closed. This should never happen"))
                    }
                }

                var lastTime = System.currentTimeMillis()
                for (update in progress) {
                    if ((System.currentTimeMillis() - lastTime) >= UPDATE_NOTIFICATION_ONLY_AFTER_MS) {
                        lastTime = System.currentTimeMillis()
                        onUpdate(update)
                    }
                }
                download.await().getOrThrow()
            }
        } catch (e: CancellationException) {
            throw e // CancellationException is normal and should not treat as error
        } catch (e: Exception) {
            return failure(e)
        }
        return success(true)
    }

    private suspend fun isDownloadPossible(app: App): kotlin.Result<Boolean> {
        if (isStopped) {
            return failure(AppUpdaterNonRetryableException("WorkRequest is stopped."))
        }
        if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            return failure(AppUpdaterRetryableException("Not enough storage is available."))
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            return failure(AppUpdaterRetryableException("Other downloads are running. "))
        }
        if (!FileDownloader.isUrlAvailable(app.findImpl().hostnameForInternetCheck)) {
            return failure(AppUpdaterRetryableException("Simple network test was not successful."))
        }
        return success(true)
    }


    companion object {
        private const val CLASS_LOGTAG = "UpdateAllAppsWorker"
        private const val WORK_MANAGER__UPDATE_ALL_APPS_WORKER = "ffupdater_update_all_apps_worker"
        private const val UPDATE_NOTIFICATION_ONLY_AFTER_MS = 3000

        fun start(context: Context) {
            logInfo("Start BackgroundWork")
            val instance = WorkManager.getInstance(context.applicationContext)
            val request = OneTimeWorkRequest.Builder(UpdateAllAppsWorker::class.java).build()
            instance.enqueueUniqueWork(WORK_MANAGER__UPDATE_ALL_APPS_WORKER, ExistingWorkPolicy.REPLACE, request)
        }

        private fun logInfo(message: String) {
            Log.i(LOG_TAG, "$CLASS_LOGTAG: $message")
        }
    }
}
