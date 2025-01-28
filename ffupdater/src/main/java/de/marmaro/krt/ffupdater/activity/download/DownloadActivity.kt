package de.marmaro.krt.ffupdater.activity.download

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
import de.marmaro.krt.ffupdater.R.string.download_activity__too_low_memory_description
import de.marmaro.krt.ffupdater.R.string.main_activity__no_unmetered_network
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.AppInstallerFactory
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.NetworkNotSuitableException
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.notification.NotificationRemover
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import de.marmaro.krt.ffupdater.storage.SystemFileManager
import de.marmaro.krt.ffupdater.utils.goneAfterExecution
import de.marmaro.krt.ffupdater.utils.ifFalse
import de.marmaro.krt.ffupdater.utils.setVisibleOrGone
import de.marmaro.krt.ffupdater.utils.visibleAfterExecution
import de.marmaro.krt.ffupdater.utils.visibleDuringExecution
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    private lateinit var installer: AppInstaller
    private lateinit var appImpl: AppBase
    private lateinit var gui: GuiHelper

    // persistent data for already running downloads
    class DownloadViewModel : ViewModel() {
        private var status: InstalledAppStatus? = null
        var deferred: Deferred<Any>? = null
        var progressChannel: Channel<DownloadStatus>? = null
        var installationFinished: Boolean = false
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
            return this.status?.app == status.app && this.status?.latestVersion == status.latestVersion && deferred?.isActive == true
        }

        fun clear() {
            status = null
            deferred = null
            progressChannel = null
            installationFinished = false
            installationSuccess = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // prevent network timeouts
        // I did not understand Android edge-to-edge completely,
        // but this should prevent elements hidden behind the system bars.
        setOnApplyWindowInsetsListener(findViewById(R.id.download_activity__main_layout)) { v: View, insets: WindowInsetsCompat ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(leftMargin, topMargin + bars.top, rightMargin, bottomMargin + bars.bottom)
            }
            insets
        }

        val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
        // check if this activity was unintentionally started again after finishing the download
        if (appFromExtras == null) {
            finish()
            return
        }

        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        if (downloadViewModel.installationFinished) {
            downloadViewModel.clear()
        }

        app = App.valueOf(appFromExtras)
        appImpl = app.findImpl()
        gui = GuiHelper(this)
        installer = AppInstallerFactory.createForegroundAppInstaller(this, app)
        lifecycle.addObserver(installer)

        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            deleteFileCache()
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            SystemFileManager.openFolder(appImpl.getApkCacheFolder(applicationContext), this)
        }
        findViewById<Button>(R.id.install_activity__retry_installation_button).setOnClickListener {
            downloadViewModel.clear()
            lifecycleScope.launch(Dispatchers.Main) {
                startInstallationProcess()
            }
        }
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }
    }

    // other download notification has been pushed
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
        // check if this activity was unintentionally started again after finishing the download
        if (appFromExtras == null) {
            finish()
            return
        }
        app = App.valueOf(appFromExtras)
        appImpl = app.findImpl()
        gui = GuiHelper(this)
        downloadViewModel.clear()
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
            deleteCachedApkFileIfSuitable(downloadViewModel.installationSuccess)
        }
        lifecycle.removeObserver(installer)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun deleteCachedApkFileIfSuitable(wasInstallationSuccessful: Boolean) {
        debug("check if the cached APK for ${app.name} should be deleted.")
        val ifInstallSuccessfulShouldApkBeDeleted = mapOf(
            true to ForegroundSettings.isDeleteUpdateIfInstallSuccessful,
            false to ForegroundSettings.isDeleteUpdateIfInstallFailed
        )
        if (ifInstallSuccessfulShouldApkBeDeleted[wasInstallationSuccessful]!!) {
            lifecycleScope.launch(Dispatchers.Main) {
                appImpl.deleteFileCache(applicationContext)
            }
        }
    }

    private suspend fun startInstallationProcess() {
        debug("start fetching, downloading and installation process")
        gui.resetGui()
        isStorageMounted().ifFalse { return }
        showWarningIfNotEnoughStorageIsAvailable()

        val status = fetchDownloadInformation() ?: return
        executeDownloadProcess(status).ifFalse { return }
        installAppWithResultProcessing(status)
    }

    private fun isStorageMounted(): Boolean {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            return true
        }
        debug("storage is not mounted")
        gui.show(R.id.externalStorageNotAccessible)
        gui.setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
        return false
    }

    private fun isNetworkSuitable(): Boolean {
        if (ForegroundSettings.isDownloadOnMeteredAllowed || !isNetworkMetered(applicationContext)) {
            return true
        }
        gui.displayFetchFailure(NetworkNotSuitableException(getString(main_activity__no_unmetered_network)))
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
            debug("fetching download information failed for ${app.name}", e)
            if (e !is DisplayableException) throw e
            gui.displayFetchFailure(e)
            null
        }
    }

    private suspend fun fetchDownloadInformationWithoutErrorChecking(): InstalledAppStatus {
        debug("fetching download information for ${app.name}")
        val source = appImpl.downloadSource
        val inProgressText = getString(download_activity__fetch_url_for_download, source)
        gui.setText(R.id.fetchUrlTextView, inProgressText)
        val finishedText = getString(download_activity__fetched_url_for_download_successfully, source)
        gui.setText(R.id.fetchedUrlSuccessTextView, finishedText)

        var status: InstalledAppStatus? = null
        findViewById<View>(R.id.fetchUrl).visibleDuringExecution {
            status = appImpl.findStatusOrUseOldCache(applicationContext)
        }
        gui.show(R.id.fetchedUrlSuccess)
        return status!!
    }

    private suspend fun executeDownloadProcess(status: InstalledAppStatus): Boolean {
        debug("check if an existing download can be reused")
        if (downloadViewModel.isDownloadForCurrentAppRunning(status)) {
            return reuseCurrentDownload(status)
        }

        debug("check if no APK file is cached")
        if (!appImpl.isApkDownloaded(applicationContext, status.latestVersion)) {
            isNetworkSuitable().ifFalse { return false }
            return startDownload(status)
        }

        debug("use cached APK file")
        val file = appImpl.getApkFile(applicationContext, status.latestVersion)
        gui.show(R.id.useCachedDownloadedApk)
        gui.setText(R.id.useCachedDownloadedApk__path, file.absolutePath)
        return true
    }

    @MainThread
    private suspend fun reuseCurrentDownload(status: InstalledAppStatus): Boolean {
        debug("start reusing existing download")
        return try {
            findViewById<View>(R.id.downloadingFile).visibleDuringExecution {
                findViewById<View>(R.id.downloadedFile).visibleAfterExecution {
                    reuseCurrentDownloadWithoutErrorChecking(status)
                }
            }

            true
        } catch (e: Exception) {
            debug("reusing the existing download of $[app.name} failed", e)
            if (e !is DisplayableException) throw e
            gui.displayDownloadFailure(status, e)
            false
        }
    }

    @MainThread
    private suspend fun reuseCurrentDownloadWithoutErrorChecking(status: InstalledAppStatus) {
        debug("reuse existing download")
        gui.setText(R.id.downloadingFileUrl, status.latestVersion.downloadUrl)
        gui.setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))
        findViewById<View>(R.id.downloadingFile).visibleDuringExecution {
            for (update in downloadViewModel.progressChannel!!) {
                withContext(Dispatchers.Main) {
                    gui.updateDownloadProgressIndication(update)
                }
            }
        }
        downloadViewModel.deferred!!.await()
    }

    @MainThread
    private suspend fun startDownload(status: InstalledAppStatus): Boolean {
        debug("start download (1/2)")
        return try {
            gui.setText(R.id.downloadingFileUrl, status.latestVersion.downloadUrl)
            gui.setText(R.id.downloadedFileUrl, status.latestVersion.downloadUrl)
            gui.setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))
            findViewById<View>(R.id.downloadingFile).visibleDuringExecution {
                findViewById<View>(R.id.downloadedFile).visibleAfterExecution {
                    downloadWithUiProgressIndication(status)
                }
            }
            true
        } catch (e: Exception) {
            debug("download failed", e)
            if (e !is DisplayableException) throw e
            gui.displayDownloadFailure(status, e)
            false
        }
    }

    @MainThread
    private suspend fun downloadWithUiProgressIndication(status: InstalledAppStatus) {
        debug("start downloading (2/2)")


        val channel = Channel<DownloadStatus>()
        val download = downloadViewModel.viewModelScope.async {
            appImpl.download(applicationContext, status.latestVersion, channel)
        }
        downloadViewModel.storeNewRunningDownload(status, download, channel)

        for (update in downloadViewModel.progressChannel!!) {
            withContext(Dispatchers.Main) {
                gui.updateDownloadProgressIndication(update)
            }
        }
        download.await()
    }

    @MainThread
    private suspend fun installAppWithResultProcessing(status: InstalledAppStatus) {
        debug("install app (1/3)")
        val success = installApp(status)

        downloadViewModel.installationFinished = true
        downloadViewModel.installationSuccess = success
        if (success) {
            appImpl.appWasInstalledCallback(applicationContext, status)
        }
        deleteCachedApkFileIfSuitable(success)

        findViewById<View>(R.id.install_activity__retry_installation).setVisibleOrGone(!success)
    }

    @MainThread
    private suspend fun installApp(status: InstalledAppStatus): Boolean {
        debug("install app (2/3)")
        return try {
            installAppWithoutErrorChecking(status)
            true
        } catch (e: InstallationFailedException) {
            debug("installation failed", e)
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            gui.displayAppInstallationFailure(e.translatedMessage, ex, appImpl)
            // hide existing background notification for applicationContext app
            NotificationRemover.removeAppStatusNotifications(applicationContext, app)
            false
        }
    }

    @MainThread
    private suspend fun installAppWithoutErrorChecking(status: InstalledAppStatus) {
        debug("install app (3/3)")
        val file = appImpl.getApkFile(applicationContext, status.latestVersion)

        var certificateHash = "error"
        findViewById<View>(R.id.installingApplication).visibleDuringExecution {
            val installResult = installer.startInstallation(this@DownloadActivity, file, appImpl)
            certificateHash = installResult.certificateHash ?: "error"
        }

        gui.show(R.id.installerSuccess, R.id.fingerprintInstalledGood)
        gui.setText(R.id.fingerprintInstalledGoodHash, certificateHash)
        if (!ForegroundSettings.isDeleteUpdateIfInstallSuccessful) {
            gui.show(R.id.install_activity__delete_cache, R.id.install_activity__open_cache_folder)
        }
        // hide existing background notification for applicationContext app
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)
    }

    private fun debug(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.d(LOG_TAG, "$LOG_PREFIX ${app.name}: $message")
        } else {
            Log.d(LOG_TAG, "$LOG_PREFIX ${app.name}: $message", throwable)
        }
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val LOG_PREFIX = "DownloadActivity"

        /**
         * Create a new InstallActivity which have to check if app is up-to-date
         */
        fun createIntent(context: Context, app: App): Intent {
            val intent = Intent(context, DownloadActivity::class.java)
            intent.putExtra(EXTRA_APP_NAME, app.name)
            return intent
        }
    }
}