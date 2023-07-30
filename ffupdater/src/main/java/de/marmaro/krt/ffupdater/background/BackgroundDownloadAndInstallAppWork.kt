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
import de.marmaro.krt.ffupdater.background.OneTimeWorkMethodResult.Companion.executeNextOneTimeDownload
import de.marmaro.krt.ffupdater.background.OneTimeWorkMethodResult.Companion.retry
import de.marmaro.krt.ffupdater.background.OneTimeWorkMethodResult.Companion.stopNextOneTimeDownload
import de.marmaro.krt.ffupdater.background.OneTimeWorkMethodResult.Companion.success
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.device.PowerUtil
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.*
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exceptions.UserInteractionIsRequiredException
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.USE_CACHE
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showDownloadFailedNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showDownloadRunningNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showInstallFailureNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showInstallSuccessNotification
import de.marmaro.krt.ffupdater.notification.NotificationBuilder.showUpdateAvailableNotification
import de.marmaro.krt.ffupdater.notification.NotificationRemover.removeDownloadRunningNotification
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.CompletableDeferred

@Keep
class BackgroundDownloadAndInstallAppWork(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context.applicationContext, workerParams) {

    override suspend fun doWork(): Result {
        return doWorkInternal()
    }

    private suspend fun doWorkInternal(): Result {
        val context = applicationContext
        val app = getApp()
        val installedAppStatus = app.findImpl().findInstalledAppStatus(context, USE_CACHE)
        Log.i(LOG_TAG, "$LOGTAG Start for ${app.name}.")

        isAppStillOutdated(installedAppStatus).onFailure { return it }
        isAppNotDownloaded(installedAppStatus).onFailure { showUpdateAvailableNotification(context, app); return it }
        isUpdateCheckAllowed().onFailure { showUpdateAvailableNotification(context, app); return it }
        isEnoughStorage().onFailure { showUpdateAvailableNotification(context, app); return it }

        try {
            showDownloadRunningNotification(context, app, null, null)
            downloadApp(installedAppStatus) {
                showDownloadRunningNotification(context, app, it.progressInPercent, it.totalMB)
            }
        } catch (e: DisplayableException) {
            showDownloadFailedNotification(context, app, e)
            return Result.success()
        } finally {
            removeDownloadRunningNotification(context, app)
        }

        shouldAppBeInstalled(app).onFailure { showUpdateAvailableNotification(context, app); return it }

        try {
            installApplication(installedAppStatus)
            showInstallSuccessNotification(context, app)
        } catch (e: UserInteractionIsRequiredException) {
            showUpdateAvailableNotification(context, app)
            return Result.success()
        } catch (e: InstallationFailedException) {
            showInstallFailureNotification(context, app, e.errorCode, e.translatedMessage, e)
            return Result.success()
        }
        return Result.success()
    }

    private fun getApp(): App {
        val appName = inputData.getString(APP_NAME_KEY)!!
        return App.valueOf(appName)
    }

    private suspend fun isAppStillOutdated(installedAppStatus: InstalledAppStatus): OneTimeWorkMethodResult {
        if (!installedAppStatus.isUpdateAvailable) {
            return executeNextOneTimeDownload("$LOGTAG ${installedAppStatus.app.name} was already updated.")
        }
        return success()
    }

    private suspend fun isAppNotDownloaded(installedAppStatus: InstalledAppStatus): OneTimeWorkMethodResult {
        val impl = installedAppStatus.app.findImpl()
        if (impl.isApkDownloaded(applicationContext, installedAppStatus.latestVersion)) {
            return executeNextOneTimeDownload("$LOGTAG ${installedAppStatus.app.name} is already downloaded.")
        }
        return success()
    }

    private fun isUpdateCheckAllowed(): OneTimeWorkMethodResult {
        return when {
            !BackgroundSettings.isUpdateCheckEnabled ->
                stopNextOneTimeDownload("$LOGTAG Background update checks are disabled.")

            !BackgroundSettings.isDownloadEnabled ->
                stopNextOneTimeDownload("$LOGTAG Background downloads are disabled.")

            FileDownloader.areDownloadsCurrentlyRunning() ->
                retry("$LOGTAG Other downloads are running.")

            !BackgroundSettings.isUpdateCheckOnMeteredAllowed && NetworkUtil.isNetworkMetered(applicationContext) ->
                retry("$LOGTAG No unmetered network available for app download.")

            else -> success()
        }
    }

    private fun isEnoughStorage(): OneTimeWorkMethodResult {
        if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            return stopNextOneTimeDownload("$LOGTAG Not enough storage is available")
        }
        return success()
    }

    private suspend fun downloadApp(installedAppStatus: InstalledAppStatus, onUpdate: (DownloadStatus) -> Unit) {
        Log.i(LOG_TAG, "$LOGTAG Download update for ${installedAppStatus.app}.")
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

    private suspend fun shouldAppBeInstalled(app: App): OneTimeWorkMethodResult {
        val installerMethod = InstallerSettings.getInstallerMethod()
        return when {
            !BackgroundSettings.isInstallationEnabled ->
                stopNextOneTimeDownload("$LOGTAG Automatic background app installation is not enabled.")

            !DeviceSdkTester.supportsAndroid12S31() && installerMethod == SESSION_INSTALLER ->
                stopNextOneTimeDownload("$LOGTAG The current installer can not update apps in the background.")

            installerMethod == NATIVE_INSTALLER ->
                stopNextOneTimeDownload("$LOGTAG The current installer can not update apps in the background.")

            BackgroundSettings.isInstallationWhenScreenOff && PowerUtil.isDeviceInteractive() ->
                retry("$LOGTAG Device is interactive. Abort background installation.")

            DeviceSdkTester.supportsAndroid14U34() && !isGentleUpdatePossible(app) ->
                retry("$LOGTAG Gentle update is not possible. Abort background installation.")

            else -> success()
        }
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
            Log.i(LOG_TAG, "$LOGTAG Can't check if $app can be gently updated.")
            gentleUpdatePossible.complete(true)
        }
        if (!value) {
            Log.i(LOG_TAG, "$LOGTAG Skip $app because it is still in use.")
        }
        return value
    }

    private suspend fun installApplication(installedAppStatus: InstalledAppStatus) {
        val app = installedAppStatus.app
        val appImpl = app.findImpl()
        val file = appImpl.getApkFile(applicationContext, installedAppStatus.latestVersion)

        try {
            Log.i(LOG_TAG, "$LOGTAG: Update/install $app.")
            val installer = AppInstaller.createBackgroundAppInstaller(app)
            installer.startInstallation(applicationContext, file)
            appImpl.appWasInstalledCallback(applicationContext, installedAppStatus)
            if (BackgroundSettings.isDeleteUpdateIfInstallSuccessful) {
                appImpl.getApkCacheFolder(applicationContext)
            }
        } catch (e: Exception) {
            if (BackgroundSettings.isDeleteUpdateIfInstallFailed) {
                app.findImpl().deleteFileCache(applicationContext)
            }
            throw e
        }
    }

    companion object {
        private const val APP_NAME_KEY = "app_name"
        private const val LOGTAG = "BackgroundDownload:"
        fun createWorkRequest(app: App): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(APP_NAME_KEY, app.name)
                .build()
            return OneTimeWorkRequest.Builder(BackgroundDownloadAndInstallAppWork::class.java)
                .setInputData(data)
                .build()
        }
    }

}