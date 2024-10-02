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
import de.marmaro.krt.ffupdater.background.exception.AppUpdaterDownloadException
import de.marmaro.krt.ffupdater.background.exception.AppUpdaterInstallationException
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.device.PowerSaveModeReceiver
import de.marmaro.krt.ffupdater.device.PowerSaveModeReceiver.PowerSaveModeDuration.ENABLED_RECENTLY
import de.marmaro.krt.ffupdater.device.PowerUtil
import de.marmaro.krt.ffupdater.installer.AppInstallerFactory
import de.marmaro.krt.ffupdater.installer.entity.Installer.NATIVE_INSTALLER
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exceptions.UserInteractionIsRequiredException
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showDownloadFailedNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showDownloadRunningNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showGeneralErrorNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showInstallFailureNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showInstallSuccessNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showNetworkErrorNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showUpdateAvailableNotification
import de.marmaro.krt.ffupdater.notification.NotificationRemover.removeDownloadRunningNotification
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import de.marmaro.krt.ffupdater.utils.WorkManagerTiming.calcBackoffTime
import kotlinx.coroutines.CompletableDeferred

@Keep
class AppUpdater(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context.applicationContext,
        workerParams) {

    override suspend fun doWork(): Result {
        try {
            return doWorkInternal()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                logWarn("Job failed. Restart in ${calcBackoffTime(runAttemptCount)}.", e)
                return Result.retry()
            }
            val exception = AppUpdaterException(e)
            logError("Job failed.", exception)
            when (e) {
                is AppUpdaterDownloadException -> showDownloadFailedNotification(applicationContext, getApp(),
                        e.getDisplayableException())

                is AppUpdaterInstallationException -> showInstallFailureNotification(applicationContext, getApp(),
                        e.getInstallationFailedException().errorCode,
                        e.getInstallationFailedException().translatedMessage, e)

                is NetworkException -> showNetworkErrorNotification(applicationContext, exception)
                else -> showGeneralErrorNotification(applicationContext, exception)
            }
            return Result.success()
        }
    }

    private suspend fun doWorkInternal(): Result {
        val app = getApp()
        logInfo("Start for ${app.name}.")

        val (returnValue, installedAppStatus) = doUpdateCheck(app)
        if (returnValue != null) {
            return returnValue
        }

        val returnValue2 = doDownload(installedAppStatus!!)
        if (returnValue2 != null) {
            return returnValue2
        }

        return doInstallation(installedAppStatus)
    }

    private suspend fun doUpdateCheck(app: App): Pair<Result?, InstalledAppStatus?> {
        if (shouldSkipDueToUpdateCheckProblems()) {
            return Result.success() to null
        }
        if (shouldRetryDueToUpdateCheckProblems(app)) {
            return Result.retry() to null
        }

        val installedAppStatus = app.findImpl()
            .findStatusOrUseRecentCache(applicationContext)
        return null to installedAppStatus
    }

    private suspend fun doDownload(installedAppStatus: InstalledAppStatus): Result? {
        if (shouldSkipDueToDownloadProblems(installedAppStatus)) {
            showUpdateAvailableNotification(applicationContext, installedAppStatus.app)
            return Result.success()
        }
        if (shouldRetryDueToDownloadProblems(installedAppStatus.app)) {
            return Result.retry()
        }

        val app = installedAppStatus.app
        val impl = app.findImpl()
        if (!(impl.isApkDownloaded(applicationContext, installedAppStatus.latestVersion))) {
            logInfo("File for ${app.name} is already downloaded")
            return null
        }

        logInfo("Start downloading ${app.name}")
        try {
            showDownloadRunningNotification(applicationContext, app, null, null)
            downloadApp(installedAppStatus) {
                showDownloadRunningNotification(applicationContext, app, it.progressInPercent, it.totalMB)
            }
        } catch (e: DisplayableException) {
            throw AppUpdaterDownloadException("Fail to download ${app.name}", e)
        } finally {
            removeDownloadRunningNotification(applicationContext, app)
        }
        return null
    }

    private suspend fun doInstallation(appStatus: InstalledAppStatus): Result {
        if (shouldSkipDueToInstallProblems()) {
            showUpdateAvailableNotification(applicationContext, appStatus.app)
            return Result.success()
        }
        if (shouldRetryDueToInstallProblems(appStatus.app)) {
            return Result.retry()
        }

        try {
            installApplication(appStatus)
            showInstallSuccessNotification(applicationContext, appStatus.app)
        } catch (e: UserInteractionIsRequiredException) {
            showUpdateAvailableNotification(applicationContext, appStatus.app)
        } catch (e: InstallationFailedException) {
            throw AppUpdaterInstallationException("Failed to install app", e)
        }
        return Result.success()
    }

    private fun getApp(): App {
        val appName = inputData.getString(APP_NAME_KEY)!!
        return App.valueOf(appName)
    }

    private fun shouldSkipDueToUpdateCheckProblems(): Boolean {
        if (PowerSaveModeReceiver.getPowerSaveModeDuration() == ENABLED_RECENTLY) {
            logInfo("Skip download because power save mode is enabled.")
            return true
        }
        if (!BackgroundSettings.isUpdateCheckEnabled) {
            logInfo("Background update check is disabled.")
            return true
        }
        return false
    }

    private suspend fun shouldRetryDueToUpdateCheckProblems(app: App): Boolean {
        if (!FileDownloader.isUrlAvailable(app.findImpl().hostnameForInternetCheck)) {
            logWarn("Simple network test ws not successful. Abort background update check.")
            return true
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            logInfo("Other downloads are running. Retry later")
            return true
        }
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && NetworkUtil.isNetworkMetered(applicationContext)) {
            logInfo("No unmetered network available for app download. Retry later.")
            return true
        }
        return false
    }

    private suspend fun doDownload(
        app: App,
        installedAppStatus: InstalledAppStatus,
        context: Context,
    ) {
        val impl = app.findImpl()
        if (!(impl.isApkDownloaded(applicationContext, installedAppStatus.latestVersion))) {
            logInfo("File for ${app.name} is already downloaded")
            return
        }

        logInfo("Start downloading ${app.name}")
        try {
            showDownloadRunningNotification(context, app, null, null)
            downloadApp(installedAppStatus) {
                showDownloadRunningNotification(context, app, it.progressInPercent, it.totalMB)
            }
        } catch (e: DisplayableException) {
            throw AppUpdaterDownloadException("Fail to download ${app.name}", e)
        } finally {
            removeDownloadRunningNotification(context, app)
        }
    }

    private fun shouldSkipDueToDownloadProblems(installedAppStatus: InstalledAppStatus): Boolean {
        if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            logInfo("Not enough storage is available")
            return true
        }
        if (!BackgroundSettings.isDownloadEnabled) {
            logInfo("Background download is disabled.")
            return true
        }
        return false
    }

    private suspend fun shouldRetryDueToDownloadProblems(app: App): Boolean {
        if (!FileDownloader.isUrlAvailable(app.findImpl().hostnameForInternetCheck)) {
            logWarn("Simple network test ws not successful. Abort background update check.")
            return true
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            logInfo("Other downloads are running. Retry later")
            return true
        }
        if (!BackgroundSettings.isUpdateCheckOnMeteredAllowed && NetworkUtil.isNetworkMetered(applicationContext)) {
            logInfo("No unmetered network available for app download. Retry later.")
            return true
        }
        return false
    }

    private suspend fun downloadApp(installedAppStatus: InstalledAppStatus, onUpdate: (DownloadStatus) -> Unit) {
        logInfo("Download update for ${installedAppStatus.app}.")
        val appImpl = installedAppStatus.app.findImpl()
        appImpl.download(applicationContext, installedAppStatus.latestVersion) { _, progressChannel ->
            var lastTime = System.currentTimeMillis()
            for (progress in progressChannel) {
                if ((System.currentTimeMillis() - lastTime) >= 1000) {
                    lastTime = System.currentTimeMillis()
                    onUpdate(progress)
                }
            }
        }
    }

    private fun shouldSkipDueToInstallProblems(): Boolean {
        if (!DeviceSdkTester.supportsAndroid12S31() && InstallerSettings.getInstallerMethod() == SESSION_INSTALLER) {
            logInfo("The current installer can not update apps in the background.")
            return true
        }
        if (InstallerSettings.getInstallerMethod() == NATIVE_INSTALLER) {
            logInfo("The current installer can not update apps in the background.")
            return true
        }
        if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            logInfo("Not enough storage is available")
            return true
        }
        if (!BackgroundSettings.isInstallationEnabled) {
            logInfo("Background installation is disabled.")
            return true
        }
        return false
    }

    private suspend fun shouldRetryDueToInstallProblems(app: App): Boolean {
        if (BackgroundSettings.isInstallationWhenScreenOff && PowerUtil.isDeviceInteractive()) {
            logInfo("Device is interactive. Retry background installation later.")
            return true
        }
        if (DeviceSdkTester.supportsAndroid14U34() && !isGentleUpdatePossible(app)) {
            logInfo("Gentle update is not possible. Retry installation later.")
            return true
        }
        return false
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

    private suspend fun installApplication(installedAppStatus: InstalledAppStatus) {
        val app = installedAppStatus.app
        val appImpl = app.findImpl()
        val file = appImpl.getApkFile(applicationContext, installedAppStatus.latestVersion)

        try {
            logInfo("Update/install $app.")
            val installer = AppInstallerFactory.createBackgroundAppInstaller(app)
            installer.startInstallation(applicationContext, file, appImpl)
            appImpl.appWasInstalledCallback(applicationContext, installedAppStatus)
            if (BackgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                appImpl.getApkCacheFolder(applicationContext)
            }
        } catch (e: Exception) {
            if (BackgroundSettings.isDeleteUpdateIfInstallFailed) {
                app.findImpl()
                    .deleteFileCache(applicationContext)
            }
            throw e
        }
    }

    companion object {
        private const val APP_NAME_KEY = "app_name"
        private const val CLASS_LOGTAG = "BackgroundDownloaderAndInstaller:"
        private const val MAX_RETRIES = 6 // waiting time of all previous retries = about 1 hour

        fun createWorkRequest(app: App): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(APP_NAME_KEY, app.name)
                .build()
            return OneTimeWorkRequest.Builder(AppUpdater::class.java)
                .setInputData(data)
                .build()
        }

        private fun logInfo(message: String) {
            Log.i(LOG_TAG, "${CLASS_LOGTAG}: $message")
        }

        private fun logWarn(message: String) {
            Log.w(LOG_TAG, "${CLASS_LOGTAG}: $message")
        }

        private fun logWarn(message: String, exception: Exception) {
            Log.w(LOG_TAG, "${CLASS_LOGTAG}: $message", exception)
        }

        private fun logError(message: String) {
            Log.e(LOG_TAG, "${CLASS_LOGTAG}: $message")
        }

        private fun logError(message: String, exception: Exception) {
            Log.e(LOG_TAG, "${CLASS_LOGTAG}: $message", exception)
        }
    }

}