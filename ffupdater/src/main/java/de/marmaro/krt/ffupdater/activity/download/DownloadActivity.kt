package de.marmaro.krt.ffupdater.activity.download

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.download_activity__download_app_with_status
import de.marmaro.krt.ffupdater.R.string.download_activity__fetch_url_for_download
import de.marmaro.krt.ffupdater.R.string.download_activity__fetched_url_for_download_successfully
import de.marmaro.krt.ffupdater.R.string.download_activity__github_rate_limit_exceeded
import de.marmaro.krt.ffupdater.R.string.download_activity__temporary_network_issue
import de.marmaro.krt.ffupdater.R.string.download_activity__too_low_memory_description
import de.marmaro.krt.ffupdater.R.string.install_activity__download_file_failed__crash_text
import de.marmaro.krt.ffupdater.R.string.main_activity__no_unmetered_network
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createForegroundAppInstaller
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.USE_EVEN_OUTDATED_CACHE
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.notification.NotificationRemover
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import de.marmaro.krt.ffupdater.storage.SystemFileManager
import de.marmaro.krt.ffupdater.utils.goneAfterExecution
import de.marmaro.krt.ffupdater.utils.ifFalse
import de.marmaro.krt.ffupdater.utils.visibleAfterExecution
import de.marmaro.krt.ffupdater.utils.visibleDuringExecution
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Activity for downloading and installing apps on devices with API Level >= 24/Nougat.
 * Reason: If have to use the DownloadManager because this is the easiest way to download the app and access it with
 * the scheme format (for example: content://downloads/all_downloads/20).
 * The DownloadManager is more difficult to use then the default java way, but the DownloadManager offers more features
 * like restarting downloads, showing the current download status etc.
 */
@Keep
class DownloadActivity : AppCompatActivity() {
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var app: App
    private lateinit var appImpl: AppBase
    private lateinit var appInstaller: AppInstaller
    private lateinit var gui: GuiHelper

    // persistent data for already running downloads
    class DownloadViewModel : ViewModel() {
        private var status: InstalledAppStatus? = null
        var deferred: Deferred<Any>? = null
        var progressChannel: Channel<DownloadStatus>? = null
        var installationSuccess: Boolean = false

        fun storeNewRunningDownload(
            status: InstalledAppStatus,
            deferred: Deferred<Any>,
            progressChannel: Channel<DownloadStatus>,
        ) {
            this.status = status
            this.deferred = deferred
            this.progressChannel = progressChannel
        }

        fun isDownloadForCurrentAppRunning(status: InstalledAppStatus): Boolean {
            return this.status == status && deferred?.isActive == true
        }

        fun clear() {
            status = null
            deferred = null
            progressChannel = null
            installationSuccess = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // prevent network timeouts

        val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
        // check if this activity was unintentionally started again after finishing the download
        if (appFromExtras == null) {
            finish()
            return
        }

        app = App.valueOf(appFromExtras)
        appImpl = app.findImpl()
        appInstaller = createForegroundAppInstaller(this, app)
        gui = GuiHelper(app, this)
        lifecycle.addObserver(appInstaller)

        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            deleteFileCache()
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            SystemFileManager.openFolder(appImpl.getApkCacheFolder(applicationContext), this)
        }
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }
    }

    private fun deleteFileCache() {
        lifecycleScope.launch(Dispatchers.Main) {
            findViewById<View>(R.id.install_activity__delete_cache).goneAfterExecution {
                app.findImpl().deleteFileCache(applicationContext)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // if the device was not rotated
        if (!isChangingConfigurations) {
            deleteCachedApkFileIfNecessary()
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun deleteCachedApkFileIfNecessary() {
        if (downloadViewModel.installationSuccess) {
            if (ForegroundSettings.isDeleteUpdateIfInstallSuccessful) {
                lifecycleScope.launch(Dispatchers.Main) {
                    appImpl.deleteFileCache(applicationContext)
                }
            }
        } else {
            if (ForegroundSettings.isDeleteUpdateIfInstallFailed) {
                lifecycleScope.launch(Dispatchers.Main) {
                    appImpl.deleteFileCache(applicationContext)
                }
            }
        }
        downloadViewModel.clear()
    }

    private suspend fun startInstallationProcess() {
        Log.d(LOG_TAG, "DownloadActivity: Start process for ${app.name}.")

        isStorageMounted().ifFalse { return }
        showWarningIfNotEnoughStorageIsAvailable()

        val status = fetchDownloadInformation() ?: return
        executeDownloadProcess(status).ifFalse { return }
        val success = installApp(status)

        downloadViewModel.installationSuccess = success
        if (success) {
            appImpl.appWasInstalledCallback(applicationContext, status)
        }
    }

    private fun isStorageMounted(): Boolean {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            return true
        }
        gui.show(R.id.externalStorageNotAccessible)
        gui.setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
        return false
    }

    private fun isNetworkSuitable(): Boolean {
        if (ForegroundSettings.isDownloadOnMeteredAllowed || !isNetworkMetered(applicationContext)) {
            return true
        }
        gui.displayFetchFailure(getString(main_activity__no_unmetered_network), null)
        return false
    }

    private fun showWarningIfNotEnoughStorageIsAvailable() {
        if (StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            return
        }
        gui.show(R.id.tooLowMemory)
        val mbs = StorageUtil.getFreeStorageInMebibytes(applicationContext)
        val message = getString(download_activity__too_low_memory_description, mbs)
        gui.setText(R.id.tooLowMemoryDescription, message)
    }

    private suspend fun fetchDownloadInformation(): InstalledAppStatus? {
        return try {
            fetchDownloadInformationWithoutErrorChecking()
        } catch (e: Exception) {
            val text = when (e) {
                is ApiRateLimitExceededException -> getString(download_activity__github_rate_limit_exceeded)
                is DisplayableException -> getString(download_activity__temporary_network_issue)
                else -> throw e
            }
            gui.displayFetchFailure(text, e)
            null
        }
    }

    private suspend fun fetchDownloadInformationWithoutErrorChecking(): InstalledAppStatus {
        val source = appImpl.downloadSource
        val inProgressText = getString(download_activity__fetch_url_for_download, source)
        val finishedText = getString(download_activity__fetched_url_for_download_successfully, source)

        gui.setText(R.id.fetchUrlTextView, inProgressText)

        var status: InstalledAppStatus? = null
        findViewById<View>(R.id.fetchUrl).visibleDuringExecution {
            status = appImpl.findInstalledAppStatus(applicationContext, USE_EVEN_OUTDATED_CACHE)
        }

        gui.show(R.id.fetchedUrlSuccess)
        gui.setText(R.id.fetchedUrlSuccessTextView, finishedText)
        return status!!
    }

    private suspend fun executeDownloadProcess(status: InstalledAppStatus): Boolean {
        if (downloadViewModel.isDownloadForCurrentAppRunning(status)) {
            return reuseCurrentDownload(status)
        }

        val appImpl = app.findImpl()
        if (appImpl.isApkDownloaded(applicationContext, status.latestVersion)) {
            Log.d(LOG_TAG, "DownloadActivity: Use APK cache of ${app.name}.")
            gui.show(R.id.useCachedDownloadedApk)
            val file = appImpl.getApkFile(applicationContext, status.latestVersion)
            gui.setText(R.id.useCachedDownloadedApk__path, file.absolutePath)
            return true
        }

        isNetworkSuitable().ifFalse { return false }
        return startDownload(status)
    }

    @MainThread
    private suspend fun reuseCurrentDownload(status: InstalledAppStatus): Boolean {
        return try {
            reuseCurrentDownloadWithoutErrorChecking(status)
            true
        } catch (e: Exception) {
            val text = when (e) {
                is NetworkException -> getString(install_activity__download_file_failed__crash_text)
                is DisplayableException -> e.message ?: e.javaClass.name
                else -> throw e
            }
            gui.displayDownloadFailure(status, text, e)
            Log.d(LOG_TAG, "DownloadActivity: Reusing failed for ${app.name}.")
            false
        }
    }

    @MainThread
    private suspend fun reuseCurrentDownloadWithoutErrorChecking(status: InstalledAppStatus) {
        Log.d(LOG_TAG, "DownloadActivity: Reuse running download of ${app.name}.")
        gui.setText(R.id.downloadingFileUrl, status.latestVersion.downloadUrl)
        gui.setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))

        findViewById<View>(R.id.downloadingFile).visibleDuringExecution {
            gui.showDownloadProgress(downloadViewModel.progressChannel!!)
            // NPE was thrown in https://github.com/Tobi823/ffupdater/issues/359 - it should be safe to ignore null values
            downloadViewModel.deferred?.await()
        }
    }

    @MainThread
    private suspend fun startDownload(status: InstalledAppStatus): Boolean {
        return try {
            startDownloadWithoutErrorChecking(status)
        } catch (e: Exception) {
            val text = when (e) {
                is NetworkException -> getString(install_activity__download_file_failed__crash_text)
                is DisplayableException -> e.message ?: e.javaClass.name
                else -> throw e
            }
            gui.displayDownloadFailure(status, text, e)
            false
        }
    }

    @MainThread
    private suspend fun startDownloadWithoutErrorChecking(status: InstalledAppStatus): Boolean {
        Log.d(LOG_TAG, "DownloadActivity: Start download of ${app.name}.")
        gui.setText(R.id.downloadingFileUrl, status.latestVersion.downloadUrl)
        gui.setText(R.id.downloadedFileUrl, status.latestVersion.downloadUrl)
        gui.setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))
        findViewById<View>(R.id.downloadingFile).visibleDuringExecution {
            findViewById<View>(R.id.downloadedFile).visibleAfterExecution {
                startDownloadInternal(status)
            }
        }
        return true
    }

    @MainThread
    private suspend fun startDownloadInternal(status: InstalledAppStatus) {
        val appImpl = app.findImpl()
        val coroutineContext = downloadViewModel.viewModelScope.coroutineContext
        withContext(coroutineContext) {
            appImpl.download(applicationContext, status.latestVersion) { deferred, progressChannel ->
                downloadViewModel.storeNewRunningDownload(status, deferred, progressChannel)
                gui.showDownloadProgress(progressChannel)
            }
        }
    }

    @MainThread
    private suspend fun installApp(status: InstalledAppStatus): Boolean {
        return try {
            installAppWithoutErrorChecking(status)
            true
        } catch (e: InstallationFailedException) {
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            gui.displayAppInstallationFailure(e.translatedMessage, ex)
            // hide existing background notification for applicationContext app
            NotificationRemover.removeAppStatusNotifications(applicationContext, app)
            false
        }
    }

    @MainThread
    private suspend fun installAppWithoutErrorChecking(status: InstalledAppStatus) {
        Log.d(LOG_TAG, "DownloadActivity: Install app ${app.name}.")
        val file = appImpl.getApkFile(applicationContext, status.latestVersion)

        var certificateHash = "error"
        findViewById<View>(R.id.installingApplication).visibleDuringExecution {
            val installResult = appInstaller.startInstallation(this@DownloadActivity, file)
            certificateHash = installResult.certificateHash ?: "error"
        }

        gui.show(R.id.installerSuccess)
        gui.show(R.id.fingerprintInstalledGood)
        gui.setText(R.id.fingerprintInstalledGoodHash, certificateHash)
        if (!ForegroundSettings.isDeleteUpdateIfInstallSuccessful) {
            gui.show(R.id.install_activity__delete_cache)
            gui.show(R.id.install_activity__open_cache_folder)
        }
        // hide existing background notification for applicationContext app
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"

        /**
         * Create a new InstallActivity which have to check if app is up-to-date
         */
        fun createIntent(context: Context, app: App): Intent {
            val intent = Intent(context, DownloadActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(EXTRA_APP_NAME, app.name)
            return intent
        }
    }
}