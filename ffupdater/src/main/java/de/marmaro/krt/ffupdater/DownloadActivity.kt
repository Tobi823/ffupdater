package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUriExposedException
import android.os.StrictMode
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R.string.*
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createForegroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.installer.exception.InstallationFailedException
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import de.marmaro.krt.ffupdater.storage.AppCache
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Activity for downloading and installing apps on devices with API Level >= 24/Nougat.
 * Reason: If have to use the DownloadManager because this is the easiest way to download the app and access it with
 * the scheme format (for example: content://downloads/all_downloads/20).
 * The DownloadManager is more difficult to use then the default java way, but the DownloadManager offers more features
 * like restarting downloads, showing the current download status etc.
 */
class DownloadActivity : AppCompatActivity() {
    private lateinit var viewModel: InstallActivityViewModel
    private lateinit var appInstaller: AppInstaller
    private lateinit var appCache: AppCache
    private lateinit var foregroundSettings: ForegroundSettingsHelper
    private lateinit var installerSettings: InstallerSettingsHelper
    private lateinit var networkSettings: NetworkSettingsHelper

    // persistent data across orientation changes
    class InstallActivityViewModel : ViewModel() {
        var app: App? = null
        var fileDownloader: FileDownloader? = null
        var appAppUpdateStatus: AppUpdateStatus? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[InstallActivityViewModel::class.java]
        if (viewModel.app == null) {
            val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
            if (appFromExtras == null) {
                // InstallActivity was unintentionally started again after finishing the download
                finish()
                return
            }
            viewModel.app = App.valueOf(appFromExtras)
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        foregroundSettings = ForegroundSettingsHelper(preferences)
        installerSettings = InstallerSettingsHelper(preferences)
        networkSettings = NetworkSettingsHelper(preferences)
        appCache = AppCache(viewModel.app!!)
        appInstaller = createForegroundAppInstaller(this, viewModel.app!!, appCache.getApkFile(this))
        lifecycle.addObserver(appInstaller)

        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            appCache.delete(this)
            hide(R.id.install_activity__delete_cache)
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            tryOpenDownloadFolderInFileManager()
        }

        // hide existing background notification for this app
        BackgroundNotificationBuilder.hideUpdateIsAvailable(this, viewModel.app!!)
        BackgroundNotificationBuilder.hideInstallationSuccess(this, viewModel.app!!)
        BackgroundNotificationBuilder.hideInstallationError(this, viewModel.app!!)

        // prevent network timeouts when the displayed is automatically turned off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }
    }

    private fun tryOpenDownloadFolderInFileManager() {
        val uri = Uri.parse("file://${appCache.getCacheFolder(this).absolutePath}/")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        val chooser = Intent.createChooser(intent, getString(download_activity__open_folder))

        if (DeviceSdkTester.supportsAndroidNougat()) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
            try {
                startActivity(chooser)
            } catch (e: FileUriExposedException) {
                Toast.makeText(this, download_activity__file_uri_exposed_toast, LENGTH_LONG)
                    .show()
            }
        } else {
            startActivity(chooser)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun show(viewId: Int) {
        findViewById<View>(viewId).visibility = View.VISIBLE
    }

    private fun hide(viewId: Int) {
        findViewById<View>(viewId).visibility = View.GONE
    }

    private fun setText(textId: Int, text: String) {
        findViewById<TextView>(textId).text = text
    }

    private suspend fun startInstallationProcess() {
        checkIfStorageIsMounted()
    }

    private suspend fun checkIfStorageIsMounted() {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            checkIfEnoughStorageAvailable()
            return
        }
        showThatExternalStorageIsNotAccessible()
    }

    private suspend fun checkIfEnoughStorageAvailable() {
        if (StorageUtil.isEnoughStorageAvailable(this)) {
            fetchDownloadInformation()
            return
        }
        show(R.id.tooLowMemory)
        val mbs = StorageUtil.getFreeStorageInMebibytes(this)
        val message = getString(download_activity__too_low_memory_description, mbs)
        setText(R.id.tooLowMemoryDescription, message)
        fetchDownloadInformation()
    }

    private suspend fun fetchDownloadInformation() {
        show(R.id.fetchUrl)
        val app = viewModel.app!!
        val downloadSource = app.impl.downloadSource
        setText(R.id.fetchUrlTextView, getString(download_activity__fetch_url_for_download, downloadSource))

        if (viewModel.appAppUpdateStatus == null) {
            // only check for updates if the cache is empty
            val updateCache = app.impl.getUpdateCache(this)
            if (updateCache != null) {
                viewModel.appAppUpdateStatus = updateCache
            } else {
                // only check for updates if network type requirements are met
                if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
                    showThatUrlFetchingFailed(getString(main_activity__no_unmetered_network))
                    return
                }

                try {
                    viewModel.appAppUpdateStatus = app.impl.checkForUpdateAsync(this).await()
                } catch (e: ApiRateLimitExceededException) {
                    showThatUrlFetchingFailed(getString(download_activity__github_rate_limit_exceeded), e)
                    return
                } catch (e: NetworkException) {
                    showThatUrlFetchingFailed(getString(download_activity__temporary_network_issue), e)
                    return
                }
            }
        }

        hide(R.id.fetchUrl)
        show(R.id.fetchedUrlSuccess)
        val finishedText = getString(download_activity__fetched_url_for_download_successfully, downloadSource)
        setText(R.id.fetchedUrlSuccessTextView, finishedText)
        val appUpdateResult = viewModel.appAppUpdateStatus!!.latestUpdate
        if (appCache.isLatestAppVersionCached(this, appUpdateResult)) {
            show(R.id.useCachedDownloadedApk)
            setText(R.id.useCachedDownloadedApk__path, appCache.getApkFile(this).absolutePath)
            installApp()
            return
        }

        if (viewModel.fileDownloader?.currentDownload != null) {
            reuseCurrentDownload()
            return
        }
        startDownload()
    }

    @MainThread
    private suspend fun startDownload() {
        if (!foregroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(this)) {
            showThatUrlFetchingFailed(getString(main_activity__no_unmetered_network))
            return
        }

        show(R.id.downloadingFile)
        val downloadUrl = viewModel.appAppUpdateStatus!!.downloadUrl
        setText(R.id.downloadingFileUrl, downloadUrl)

        val fileDownloader = FileDownloader(networkSettings)
        viewModel.fileDownloader = fileDownloader
        fileDownloader.onProgress = { percentage, mb ->
            runOnUiThread {
                val status = if (percentage != null) {
                    findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = percentage
                    "${percentage}%"
                } else {
                    "${mb}MB"
                }
                val display = getString(download_activity__download_app_with_status, status)
                setText(R.id.downloadingFileText, display)
            }
        }

        val display = getString(download_activity__download_app_with_status, "")
        setText(R.id.downloadingFileText, display)

        appCache.delete(this)

        // this coroutine should survive a screen rotation and should live as long as the view model
        try {
            val file = appCache.getFileForDownloader(this)
            withContext(viewModel.viewModelScope.coroutineContext) {
                fileDownloader.downloadFileAsync(downloadUrl, file).await()
            }
            hide(R.id.downloadingFile)
            show(R.id.downloadedFile)
            setText(R.id.downloadedFileUrl, downloadUrl)
            postProcessDownload()
        } catch (e: NetworkException) {
            hide(R.id.downloadingFile)
            show(R.id.downloadedFile)
            setText(R.id.downloadedFileUrl, downloadUrl)
            showThatDownloadFailed(e)
        }
    }

    @MainThread
    private suspend fun reuseCurrentDownload() {
        val downloadUrl = viewModel.appAppUpdateStatus!!.downloadUrl
        show(R.id.downloadingFile)
        setText(R.id.downloadingFileUrl, downloadUrl)
        val fileDownloader = requireNotNull(viewModel.fileDownloader)
        fileDownloader.onProgress = { percentage, mb ->
            runOnUiThread {
                val status = if (percentage != null) {
                    findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = percentage
                    "${percentage}%"
                } else {
                    "${mb}MB"
                }
                val display = getString(download_activity__download_app_with_status, status)
                setText(R.id.downloadingFileText, display)
            }
        }

        try {
            fileDownloader.currentDownload?.await()
            postProcessDownload()
        } catch (e: NetworkException) {
            showThatDownloadFailed(e)
        }
    }

    @MainThread
    private suspend fun postProcessDownload() {
        val app = viewModel.app!!
        // if the download was an ZIP archive, then extract the APK file
        if (!app.impl.isDownloadAnApkFile()) {
            val zipArchive = appCache.getZipFile(this)
            val apkFile = appCache.getApkFile(this)
            withContext(Dispatchers.IO) {
                async {
                    app.impl.convertZipArchiveToApkFile(zipArchive, apkFile)
                    zipArchive.delete()
                }
            }.await()
        }
        installApp()
    }

    @MainThread
    private suspend fun installApp() {
        show(R.id.installingApplication)
        val app = viewModel.app!!
        try {
            val certificateHash = appInstaller.installAsync(this).await().certificateHash ?: ""
            showThatAppIsInstalled(certificateHash)
        } catch (e: InstallationFailedException) {
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            showThatAppInstallationFailed(e.errorMessage, ex)
        } finally {
            // hide existing background notification for this app
            BackgroundNotificationBuilder.hideUpdateIsAvailable(this, app)
            BackgroundNotificationBuilder.hideInstallationSuccess(this, app)
            BackgroundNotificationBuilder.hideInstallationError(this, app)
        }
    }

    @MainThread
    private fun showThatAppInstallationFailed(errorMessage: String, exception: Exception) {
        hide(R.id.installingApplication)
        show(R.id.install_activity__exception)
        show(R.id.install_activity__exception__description)
        setText(
            R.id.install_activity__exception__text,
            getString(application_installation_was_not_successful)
        )
        if (installerSettings.getInstaller() == SESSION_INSTALLER) {
            show(R.id.install_activity__different_installer_info)
        }

        findViewById<TextView>(R.id.install_activity__exception__description).text = errorMessage
        findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = getString(crash_report__explain_text__download_activity_install_file)
            val intent = CrashReportActivity.createIntent(this, exception, description)
            startActivity(intent)
        }

        val cacheFolder = appCache.getCacheFolder(this).absolutePath
        setText(R.id.install_activity__cache_folder_path, cacheFolder)
        if (foregroundSettings.isDeleteUpdateIfInstallFailed) {
            appCache.delete(this)
        } else {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @MainThread
    private fun showThatAppIsInstalled(certificateHash: String) {
        hide(R.id.installingApplication)
        show(R.id.installerSuccess)
        show(R.id.fingerprintInstalledGood)
        setText(R.id.fingerprintInstalledGoodHash, certificateHash)
        viewModel.app!!.impl.appIsInstalled(this, viewModel.appAppUpdateStatus!!)
        if (foregroundSettings.isDeleteUpdateIfInstallSuccessful) {
            appCache.delete(this)
        } else {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @MainThread
    private fun showThatExternalStorageIsNotAccessible() {
        show(R.id.externalStorageNotAccessible)
        setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @MainThread
    private fun showThatDownloadFailed(exception: Exception) {
        val downloadUrl = viewModel.appAppUpdateStatus!!.downloadUrl
        hide(R.id.downloadingFile)
        show(R.id.downloadFileFailed)
        setText(R.id.downloadFileFailedUrl, downloadUrl)
        findViewById<TextView>(R.id.downloadFileFailedShowException).setOnClickListener {
            val description = getString(crash_report__explain_text__download_activity_download_file)
            val intent = CrashReportActivity.createIntent(this, exception, description)
            startActivity(intent)
        }
        appCache.delete(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @MainThread
    private fun showThatUrlFetchingFailed(message: String, exception: Exception? = null) {
        hide(R.id.fetchUrl)
        show(R.id.install_activity__exception)
        setText(R.id.install_activity__exception__text, message)
        if (exception == null) {
            hide(R.id.install_activity__exception__show_button)
            return
        }
        findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = getString(crash_report__explain_text__download_activity_fetching_url)
            val intent = CrashReportActivity.createIntent(this, exception, description)
            startActivity(intent)
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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