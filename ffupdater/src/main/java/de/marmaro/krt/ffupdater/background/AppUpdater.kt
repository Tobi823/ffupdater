package de.marmaro.krt.ffupdater.background

import android.content.Context
import android.content.pm.PackageInstaller.InstallConstraints.GENTLE_UPDATE
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.background.exception.AppUpdaterNonRetryableException
import de.marmaro.krt.ffupdater.background.exception.AppUpdaterRetryableException
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.device.PowerSaveModeReceiver
import de.marmaro.krt.ffupdater.device.PowerSaveModeReceiver.PowerSaveModeDuration.ENABLED_RECENTLY
import de.marmaro.krt.ffupdater.device.PowerUtil
import de.marmaro.krt.ffupdater.installer.AppInstallerFactory
import de.marmaro.krt.ffupdater.installer.entity.Installer.NATIVE_INSTALLER
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showDownloadRunningNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showGeneralErrorNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showInstallSuccessNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showUpdateAvailableNotification
import de.marmaro.krt.ffupdater.notification.NotificationRemover
import de.marmaro.krt.ffupdater.notification.NotificationRemover.removeDownloadRunningNotification
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration.ofDays
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

@Keep
class AppUpdater(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private var showUpdateNotification = false

    override suspend fun doWork(): Result {
        logInfo("Start doWork for ${getApp()}")

        return doWorkInternal().fold(onSuccess = { Result.success() }, onFailure = {
            logError("Failed to update app", it)
            if (showUpdateNotification && !(it is AppUpdaterRetryableException && runAttemptCount < MAX_RETRIES)) {
                showUpdateAvailableNotification(applicationContext, getApp())
            }
            if (it is AppUpdaterRetryableException) {
                return@fold getLimitedRetryValue(Result.success(), it)
            }
            if (it is AppUpdaterNonRetryableException) {
                return@fold Result.success()
            }
            // unexpected exception: do a limited retry
            return@fold getLimitedRetryValue(Result.failure(), it)
        })
    }

    private suspend fun doWorkInternal(): kotlin.Result<Boolean> {
        val app = getApp()
        logInfo("Start for ${app.name}.")
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)

        val installedAppStatus = doUpdateCheck(app).onFailure { return failure(it) }.getOrThrow()
        // update is available because doUpdateCheck will fail if no update is available
        showUpdateNotification = true
        app.findImpl().deleteFileCacheExceptLatest(applicationContext, installedAppStatus.latestVersion)
        doDownload(installedAppStatus).onFailure { return failure(it) }
        doInstallation(installedAppStatus).onFailure { return failure(it) }
        return success(true)
    }

    private suspend fun doUpdateCheck(app: App): kotlin.Result<InstalledAppStatus> {
        isUpdateCheckPossible(app).onFailure { return failure(it) }

        val installedAppStatus = try {
            app.findImpl().findStatusOrUseRecentCache(applicationContext)
        } catch (e: Exception) {
            return failure(e)
        }
        if (!installedAppStatus.isUpdateAvailable) {
            return failure(AppUpdaterNonRetryableException("No update available for ${app.name}."))
        }
        return success(installedAppStatus)
    }

    private suspend fun doDownload(installedAppStatus: InstalledAppStatus): kotlin.Result<Boolean> {
        isDownloadPossible(installedAppStatus.app).onFailure { return failure(it) }
        val app = installedAppStatus.app
        if (app.findImpl().isApkDownloaded(applicationContext, installedAppStatus.latestVersion)) {
            logInfo("File for ${app.name} is already downloaded")
            return success(true)
        }

        isDownloadPossible(installedAppStatus.app).onFailure { return failure(it) }

        logInfo("Start downloading ${app.name}")
        showDownloadRunningNotification(applicationContext, app, null, null)
        val result = downloadApp(installedAppStatus) {
            withContext(Dispatchers.Main) {
                showDownloadRunningNotification(applicationContext, app, it.progressInPercent, it.totalMB)
            }
        }
        removeDownloadRunningNotification(applicationContext, app)
        return result
    }

    private suspend fun doInstallation(appStatus: InstalledAppStatus): kotlin.Result<Boolean> {
        isInstallationPossible(appStatus.app).onFailure { return failure(it) }

        return installApplication(appStatus).onSuccess {
            showInstallSuccessNotification(applicationContext, appStatus.app)
            return success(true)
        }.onFailure {
            showUpdateAvailableNotification(applicationContext, appStatus.app)
            return failure(AppUpdaterNonRetryableException("Installation failed", it))
        }
    }

    private fun getApp(): App {
        val appName = inputData.getString(APP_NAME_KEY)!!
        return App.valueOf(appName)
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
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            return failure(AppUpdaterRetryableException("No unmetered network available for app download. Retry later."))
        }
        if (!BackgroundSettings.isUpdateCheckEnabled) {
            return failure(AppUpdaterNonRetryableException("Background update check is disabled."))
        }
        if (NetworkUtil.isDataSaverEnabled(applicationContext)) {
            return failure(AppUpdaterNonRetryableException("Data saver is enabled."))
        }
        if (PowerSaveModeReceiver.getPowerSaveModeDuration() == ENABLED_RECENTLY) {
            return failure(AppUpdaterNonRetryableException("Power save mode is enabled."))
        }
        if (PowerUtil.isBatteryLow()) {
            return failure(AppUpdaterNonRetryableException("Battery is low."))
        }
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            return failure(AppUpdaterNonRetryableException("No unmetered network is available."))
        }
        if (BackgroundSettings.isUpdateCheckOnlyAllowedWhenDeviceIsIdle && PowerUtil.isDeviceInteractive()) {
            return failure(AppUpdaterNonRetryableException("Device is not idle."))
        }
        return success(true)
    }

    private suspend fun isDownloadPossible(app: App): kotlin.Result<Boolean> {
        if (isStopped) {
            return failure(AppUpdaterNonRetryableException("WorkRequest is stopped."))
        }
        if (!BackgroundSettings.isDownloadEnabled) {
            return failure(AppUpdaterNonRetryableException("Background download is disabled."))
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
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            return failure(AppUpdaterRetryableException("No unmetered network available for app download."))
        }
        return success(true)
    }

    private suspend fun downloadApp(
        installedAppStatus: InstalledAppStatus, onUpdate: suspend (DownloadStatus) -> Unit
    ): kotlin.Result<Boolean> {
        logInfo("Download update for ${installedAppStatus.app}.")
        val appImpl = installedAppStatus.app.findImpl()
        try {
            coroutineScope {
                logInfo("Download update for ${installedAppStatus.app}.")
                val progress = Channel<DownloadStatus>()
                val download = async { // capture exception so that CrashListener will not caught it
                    try {
                        appImpl.download(applicationContext, installedAppStatus.latestVersion, progress)
                        return@async success(true)
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
        } catch (e: Exception) {
            return failure(e)
        }
        return success(true)
    }

    private suspend fun isInstallationPossible(app: App): kotlin.Result<Boolean> {
        if (isStopped) {
            return failure(AppUpdaterNonRetryableException("WorkRequest is stopped."))
        }
        if (!DeviceSdkTester.supportsAndroid12S31() && InstallerSettings.getInstallerMethod() == SESSION_INSTALLER) {
            return failure(AppUpdaterNonRetryableException("The current installer can not update apps in the background."))
        }
        if (InstallerSettings.getInstallerMethod() == NATIVE_INSTALLER) {
            return failure(AppUpdaterNonRetryableException("The current installer can not update apps in the background."))
        }
        if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            return failure(AppUpdaterNonRetryableException("Not enough storage is available."))
        }
        if (!BackgroundSettings.isInstallationEnabled) {
            return failure(AppUpdaterNonRetryableException("Background installation is disabled."))
        }
        if (BackgroundSettings.isInstallationWhenScreenOff && PowerUtil.isDeviceInteractive()) {
            return failure(AppUpdaterRetryableException("Device is interactive. Retry background installation later."))
        }
        if (DeviceSdkTester.supportsAndroid14U34() && !isGentleUpdatePossible(app)) {
            return failure(AppUpdaterRetryableException("Gentle update is not possible. Retry installation later."))
        }
        return success(true)
    }


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun isGentleUpdatePossible(app: App): Boolean {
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
            logInfo("Can't check if $app can be gently updated.")
            gentleUpdatePossible.complete(true)
        }
        if (!value) {
            logInfo("Skip $app because it is still in use.")
        }
        return value
    }

    private suspend fun installApplication(installedAppStatus: InstalledAppStatus): kotlin.Result<Boolean> {
        val app = installedAppStatus.app
        val appImpl = app.findImpl()
        val file = appImpl.getApkFile(applicationContext, installedAppStatus.latestVersion)

        try {
            logInfo("Update/install $app.")
            val installer = AppInstallerFactory.createBackgroundAppInstaller()
            installer.startInstallation(applicationContext, file, appImpl)
            appImpl.appWasInstalledCallback(applicationContext, installedAppStatus)
            if (BackgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                appImpl.getApkCacheFolder(applicationContext)
            }
            return success(true)
        } catch (e: Exception) {
            if (BackgroundSettings.isDeleteUpdateIfInstallFailed) {
                app.findImpl().deleteFileCache(applicationContext)
            }
            return failure(e)
        }
    }

    private fun getLimitedRetryValue(valueAfterTooManyRetries: Result, exception: Throwable): Result {
        if (runAttemptCount < MAX_RETRIES) {
            return Result.retry()
        } else {
            if (unsuccessfulBackgroundCheckForLongTime()) {
                showGeneralErrorNotification(applicationContext, exception)
            }
            return valueAfterTooManyRetries
        }
    }

    private fun unsuccessfulBackgroundCheckForLongTime(): Boolean {
        return (DataStoreHelper.getDurationSinceAllAppsHasBeenChecked() ?: return false) >= ERROR_IGNORE_TIMESPAN
    }

    companion object {
        private const val APP_NAME_KEY = "app_name"
        private const val CLASS_LOGTAG = "AppUpdater:"
        private const val MAX_RETRIES = 5 // total of 15.5min waiting time (30s + 60s + 120s + 240s + 480s)
        private val ERROR_IGNORE_TIMESPAN = ofDays(2)
        private const val UPDATE_NOTIFICATION_ONLY_AFTER_MS = 3000

        fun createWorkRequest(app: App): OneTimeWorkRequest {
            val data = Data.Builder().putString(APP_NAME_KEY, app.name).build()
            return OneTimeWorkRequest.Builder(AppUpdater::class.java).setInputData(data).addTag(app.name).build()
        }

        private fun logInfo(message: String) {
            Log.i(LOG_TAG, "${CLASS_LOGTAG}: $message")
        }

        private fun logError(message: String, exception: Throwable) {
            Log.e(LOG_TAG, "${CLASS_LOGTAG}: $message", exception)
        }
    }

}