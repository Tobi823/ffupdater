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
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationRemover
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import de.marmaro.krt.ffupdater.storage.StorageUtil
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
class DownloadActivity : AppCompatActivity() {
    private lateinit var viewModel: InstallActivityViewModel
    private lateinit var appInstaller: AppInstaller
    private lateinit var foregroundSettings: ForegroundSettingsHelper
    private lateinit var installerSettings: InstallerSettingsHelper
    private lateinit var networkSettings: NetworkSettingsHelper
    private val notificationRemover = BackgroundNotificationRemover.INSTANCE

    // persistent data across orientation changes
    class InstallActivityViewModel : ViewModel() {
        var app: App? = null
        var appAppUpdateStatus: AppUpdateStatus? = null

        var downloadDeferred: Deferred<Any>? = null
        var downloadProgressChannel: Channel<FileDownloader.DownloadStatus>? = null

        var deleteDownloadedFileWhenOnPause = false

        fun clear() {
            app = null
            appAppUpdateStatus = null
            downloadDeferred = null
            downloadProgressChannel = null
            deleteDownloadedFileWhenOnPause = false
        }
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
        if (intent?.extras?.getBoolean(EXTRA_FRESH_START, false) == true) {
            intent?.removeExtra(EXTRA_FRESH_START)
            viewModel.clear()

            val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
            if (appFromExtras == null) {
                // InstallActivity was unintentionally started again after finishing the download
                finish()
                return
            }
            viewModel.app = App.valueOf(appFromExtras)
        }
        val app = viewModel.app!!

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        foregroundSettings = ForegroundSettingsHelper(preferences)
        installerSettings = InstallerSettingsHelper(preferences)
        networkSettings = NetworkSettingsHelper(preferences)
        appInstaller = createForegroundAppInstaller(this, app, app.downloadedFileCache.getApkFile(this))
        lifecycle.addObserver(appInstaller)

        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            app.downloadedFileCache.deleteApkFile(this)
            hide(R.id.install_activity__delete_cache)
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            tryOpenDownloadFolderInFileManager()
        }

        // hide existing background notification for this app
        notificationRemover.removeAppStatusNotifications(this, app)

        // prevent network timeouts when the displayed is automatically turned off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }
    }

    override fun onPause() {
        super.onPause()
        // only delete a successfully downloaded file if the activity is closed
        // (to prevent unnecessary downloads when the screen is rotated)
        if (viewModel.deleteDownloadedFileWhenOnPause) {
            viewModel.app!!.downloadedFileCache.deleteApkFile(this)
        }
    }

    private fun tryOpenDownloadFolderInFileManager() {
        val absolutePath = viewModel.app!!.downloadedFileCache.getCacheFolder(this).absolutePath
        val uri = Uri.parse("file://$absolutePath/")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        val chooser = Intent.createChooser(intent, getString(download_activity__open_folder))

        if (DeviceSdkTester.INSTANCE.supportsAndroidNougat()) {
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
            val updateCache = app.metadataCache.getCachedOrNullIfOutdated(this)
            if (updateCache != null) {
                viewModel.appAppUpdateStatus = updateCache
            } else {
                // only check for updates if network type requirements are met
                if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
                    showThatUrlFetchingFailed(getString(main_activity__no_unmetered_network))
                    return
                }

                try {
                    viewModel.appAppUpdateStatus = app.metadataCache.getCachedOrFetchIfOutdated(this)
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
        if (app.downloadedFileCache.isLatestAppVersionCached(this, appUpdateResult)) {
            show(R.id.useCachedDownloadedApk)
            setText(R.id.useCachedDownloadedApk__path, app.downloadedFileCache.getApkFile(this).absolutePath)
            installApp()
            return
        }

        if (viewModel.downloadDeferred?.isActive == true) {
            reuseCurrentDownload()
            return
        }
        startDownload()
    }

    @MainThread
    private suspend fun startDownload() {
        val app = viewModel.app!!
        if (!foregroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(this)) {
            showThatUrlFetchingFailed(getString(main_activity__no_unmetered_network))
            return
        }

        show(R.id.downloadingFile)
        val downloadUrl = viewModel.appAppUpdateStatus!!.downloadUrl
        setText(R.id.downloadingFileUrl, downloadUrl)

        val fileDownloader = FileDownloader(networkSettings)
        val display = getString(download_activity__download_app_with_status, "")
        setText(R.id.downloadingFileText, display)

        app.downloadedFileCache.deleteApkFile(this)

        // this coroutine should survive a screen rotation and should live as long as the view model
        try {
            val file = app.downloadedFileCache.getFileForDownloader(this)
            val (deferred, progressChannel) = withContext(viewModel.viewModelScope.coroutineContext) {
                // run async with await later
                fileDownloader.downloadBigFileAsync(downloadUrl, file)
            }
            viewModel.downloadDeferred = deferred
            viewModel.downloadProgressChannel = progressChannel

            for (progress in progressChannel) {
                progress.progressInPercent
                    ?.also { findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = it }

                val textStatus = when {
                    progress.progressInPercent != null -> "${progress.progressInPercent}%"
                    else -> "${progress.totalMB}MB"
                }
                setText(
                    R.id.downloadingFileText,
                    getString(download_activity__download_app_with_status, textStatus)
                )
            }
            deferred.await()

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

        for (progress in viewModel.downloadProgressChannel!!) {
            progress.progressInPercent
                ?.also { findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = it }

            val textStatus = progress.progressInPercent
                ?.let { "$it%" }
                ?: "${progress.totalMB}MB"
            setText(
                R.id.downloadingFileText,
                getString(download_activity__download_app_with_status, textStatus)
            )
        }

        try {
            viewModel.downloadDeferred!!.await()
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
            app.downloadedFileCache.convertZipArchiveToApkFile(this)
            app.downloadedFileCache.deleteZipFile(this)
        }
        installApp()
    }

    @MainThread
    private suspend fun installApp() {
        show(R.id.installingApplication)
        val app = viewModel.app!!
        try {
            val certificateHash = appInstaller.startInstallation(this).certificateHash ?: ""
            showThatAppIsInstalled(certificateHash)
        } catch (e: InstallationFailedException) {
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            showThatAppInstallationFailed(e.translatedMessage, ex)
        } finally {
            // hide existing background notification for this app
            notificationRemover.removeAppStatusNotifications(this, app)
        }
    }

    @MainThread
    private fun showThatAppIsInstalled(certificateHash: String) {
        hide(R.id.installingApplication)
        show(R.id.installerSuccess)
        show(R.id.fingerprintInstalledGood)
        setText(R.id.fingerprintInstalledGoodHash, certificateHash)
        viewModel.app!!.impl.appIsInstalledCallback(this, viewModel.appAppUpdateStatus!!)
        if (foregroundSettings.isDeleteUpdateIfInstallSuccessful) {
            viewModel.deleteDownloadedFileWhenOnPause = true
        } else {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
        cleanupUi()
    }

    @MainThread
    private fun showThatAppInstallationFailed(errorMessage: String, exception: Exception) {
        val app = viewModel.app!!
        hide(R.id.installingApplication)
        show(R.id.install_activity__exception)
        show(R.id.install_activity__exception__description)
        setText(
            R.id.install_activity__exception__text,
            getString(application_installation_was_not_successful)
        )
        if (installerSettings.getInstallerMethod() == SESSION_INSTALLER) {
            show(R.id.install_activity__different_installer_info)
        }

        findViewById<TextView>(R.id.install_activity__exception__description).text = errorMessage
        findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = getString(crash_report__explain_text__download_activity_install_file)
            val intent = CrashReportActivity.createIntent(this, exception, description)
            startActivity(intent)
        }

        val cacheFolder = app.downloadedFileCache.getCacheFolder(this).absolutePath
        setText(R.id.install_activity__cache_folder_path, cacheFolder)
        if (foregroundSettings.isDeleteUpdateIfInstallFailed) {
            app.downloadedFileCache.deleteApkFile(this)
        } else {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
        cleanupUi()
    }

    @MainThread
    private fun showThatExternalStorageIsNotAccessible() {
        show(R.id.externalStorageNotAccessible)
        setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
        cleanupUi()
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
        viewModel.app!!.downloadedFileCache.deleteApkFile(this)
        cleanupUi()
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
        cleanupUi()
    }

    @MainThread
    private fun cleanupUi() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.clear()
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_FRESH_START = "fresh_start"

        /**
         * Create a new InstallActivity which have to check if app is up-to-date
         */
        fun createIntent(context: Context, app: App): Intent {
            val intent = Intent(context, DownloadActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(EXTRA_APP_NAME, app.name)
            intent.putExtra(EXTRA_FRESH_START, true)
            return intent
        }
    }
}