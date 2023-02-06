package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUriExposedException
import android.os.StrictMode
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
import de.marmaro.krt.ffupdater.installer.ApkChecker
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createForegroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
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
    private lateinit var foregroundSettings: ForegroundSettingsHelper
    private lateinit var installerSettings: InstallerSettingsHelper
    private lateinit var networkSettings: NetworkSettingsHelper
    private val notificationRemover = BackgroundNotificationRemover.INSTANCE

    private lateinit var app: App
    private lateinit var appUpdateStatus: AppUpdateStatus
    private lateinit var appInstaller: AppInstaller

    // persistent data for already running downloads
    class InstallActivityViewModel : ViewModel() {
        private var downloadApp: App? = null
        var downloadDeferred: Deferred<Any>? = null
        var downloadProgressChannel: Channel<FileDownloader.DownloadStatus>? = null

        fun storeNewRunningDownload(
            app: App,
            deferred: Deferred<Any>,
            progressChannel: Channel<FileDownloader.DownloadStatus>,
        ) {
            downloadApp = app
            downloadDeferred = deferred
            downloadProgressChannel = progressChannel
        }

        fun isDownloadForCurrentAppRunning(currentApp: App): Boolean {
            return downloadApp == currentApp && downloadDeferred?.isActive == true
        }

        fun clear() {
            downloadApp = null
            downloadDeferred = null
            downloadProgressChannel = null
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

        val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
        if (appFromExtras == null) {
            // InstallActivity was unintentionally started again after finishing the download
            finish()
            return
        }
        app = App.valueOf(appFromExtras)

        viewModel = ViewModelProvider(this)[InstallActivityViewModel::class.java]

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        foregroundSettings = ForegroundSettingsHelper(preferences)
        installerSettings = InstallerSettingsHelper(preferences)
        networkSettings = NetworkSettingsHelper(preferences)

        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            app.downloadedFileCache.deleteAllApkFileForThisApp(this)
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

        appInstaller = createForegroundAppInstaller(this, app)
        lifecycle.addObserver(appInstaller)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isChangingConfigurations) {
            // if the device is not rotated, delete information about the download to allow a new download
            // next time
            viewModel.clear()
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun tryOpenDownloadFolderInFileManager() {
        val absolutePath = app.downloadedFileCache.getCacheFolder(this).absolutePath
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
        if (!checkIfStorageIsMounted()) return
        checkIfEnoughStorageAvailable()

        if (!fetchDownloadInformationOrUseCache()) return

        when {
            viewModel.isDownloadForCurrentAppRunning(app) -> {
                if (!reuseCurrentDownload()) return
                postProcessDownload()
            }
            app.downloadedFileCache.isApkFileCached(this, appUpdateStatus.latestUpdate) -> {
                show(R.id.useCachedDownloadedApk)
                val file = app.downloadedFileCache.getApkFile(this, appUpdateStatus.latestUpdate)
                setText(R.id.useCachedDownloadedApk__path, file.absolutePath)
            }
            else -> {
                if (!startDownload()) return
                postProcessDownload()
            }
        }

        if (installApp()) {
            app.impl.appIsInstalledCallback(this, appUpdateStatus)
            if (foregroundSettings.isDeleteUpdateIfInstallSuccessful) {
                app.downloadedFileCache.deleteAllApkFileForThisApp(this)
            }
        } else {
            if (foregroundSettings.isDeleteUpdateIfInstallFailed) {
                app.downloadedFileCache.deleteAllApkFileForThisApp(this)
            }
        }
    }

    private fun checkIfStorageIsMounted(): Boolean {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            show(R.id.externalStorageNotAccessible)
            setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
            return false
        }
        return true
    }

    private fun checkIfEnoughStorageAvailable() {
        if (!StorageUtil.isEnoughStorageAvailable(this)) {
            show(R.id.tooLowMemory)
            val mbs = StorageUtil.getFreeStorageInMebibytes(this)
            val message = getString(download_activity__too_low_memory_description, mbs)
            setText(R.id.tooLowMemoryDescription, message)
        }
    }

    private suspend fun fetchDownloadInformationOrUseCache(): Boolean {
        show(R.id.fetchUrl)
        val downloadSource = app.impl.downloadSource
        setText(R.id.fetchUrlTextView, getString(download_activity__fetch_url_for_download, downloadSource))

        var status = app.metadataCache.getCachedOrNullIfOutdated(this)
        if (status == null) {
            status = fetchDownloadInformationHelper()
        }
        hide(R.id.fetchUrl)
        if (status == null) {
            return false
        }
        appUpdateStatus = status

        show(R.id.fetchedUrlSuccess)
        val finishedText = getString(download_activity__fetched_url_for_download_successfully, downloadSource)
        setText(R.id.fetchedUrlSuccessTextView, finishedText)
        return true
    }

    private suspend fun fetchDownloadInformationHelper(): AppUpdateStatus? {
        // only check for updates if network type requirements are met
        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            displayFetchFailure(getString(main_activity__no_unmetered_network))
            return null
        }

        try {
            return app.metadataCache.getCachedOrFetchIfOutdated(this)
        } catch (e: ApiRateLimitExceededException) {
            displayFetchFailure(getString(download_activity__github_rate_limit_exceeded), e)
        } catch (e: NetworkException) {
            displayFetchFailure(getString(download_activity__temporary_network_issue), e)
        }
        return null
    }

    @MainThread
    private suspend fun startDownload(): Boolean {
        if (!foregroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(this)) {
            displayFetchFailure(getString(main_activity__no_unmetered_network))
            return false
        }

        show(R.id.downloadingFile)
        setText(R.id.downloadingFileUrl, appUpdateStatus.latestUpdate.downloadUrl)
        setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status, ""))

        val fileDownloader = FileDownloader(networkSettings)
        app.downloadedFileCache.deleteAllApkFileForThisApp(this)

        // this coroutine should survive a screen rotation and should live as long as the view model
        val latestUpdate = appUpdateStatus.latestUpdate
        val file = app.downloadedFileCache.getApkOrZipTargetFileForDownload(this, latestUpdate)

        try {
            val (deferred, progressChannel) = withContext(viewModel.viewModelScope.coroutineContext) {
                // run async with await later
                fileDownloader.downloadBigFileAsync(appUpdateStatus.latestUpdate.downloadUrl, file)
            }
            viewModel.storeNewRunningDownload(app, deferred, progressChannel)

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

            // I suspect that sometimes the server offers the wrong file for download
            ApkChecker.throwIfDownloadedFileHasDifferentSize(file, latestUpdate)
            val apkFile = app.downloadedFileCache.getApkFile(this@DownloadActivity, latestUpdate)
            ApkChecker.throwIfApkFileIsNoValidZipFile(apkFile)
            return true
        } catch (e: NetworkException) {
            displayDownloadFailure(getString(install_activity__download_file_failed__crash_text), e)
            app.downloadedFileCache.deleteAllApkFileForThisApp(this)
        } catch (e: FFUpdaterException) {
            displayDownloadFailure(e.message ?: e.javaClass.name, e)
            app.downloadedFileCache.deleteAllApkFileForThisApp(this)
        } finally {
            hide(R.id.downloadingFile)
            show(R.id.downloadedFile)
            setText(R.id.downloadedFileUrl, appUpdateStatus.latestUpdate.downloadUrl)
        }
        return false
    }

    @MainThread
    private suspend fun reuseCurrentDownload(): Boolean {
        show(R.id.downloadingFile)
        val latestUpdate = appUpdateStatus.latestUpdate
        setText(R.id.downloadingFileUrl, latestUpdate.downloadUrl)

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

            // I suspect that sometimes the server offers the wrong file for download
            val file = app.downloadedFileCache.getApkOrZipTargetFileForDownload(this, latestUpdate)
            ApkChecker.throwIfDownloadedFileHasDifferentSize(file, latestUpdate)
            val apkFile = app.downloadedFileCache.getApkFile(this@DownloadActivity, latestUpdate)
            ApkChecker.throwIfApkFileIsNoValidZipFile(apkFile)
            return true
        } catch (e: NetworkException) {
            displayDownloadFailure(getString(install_activity__download_file_failed__crash_text), e)
        } catch (e: FFUpdaterException) {
            displayDownloadFailure(e.message ?: e.javaClass.name, e)
        } finally {
            hide(R.id.downloadingFile)
        }
        return false
    }

    @MainThread
    private suspend fun postProcessDownload() {
        // if the download was an ZIP archive, then extract the APK file
        if (app.impl.isAppPublishedAsZipArchive()) {
            app.downloadedFileCache.extractApkFromZipArchive(this, appUpdateStatus.latestUpdate)
            app.downloadedFileCache.deleteZipFile(this)
        }
        app.downloadedFileCache.deleteAllExceptLatestApkFile(this, appUpdateStatus.latestUpdate)
    }

    @MainThread
    private suspend fun installApp(): Boolean {
        show(R.id.installingApplication)
        val file = app.downloadedFileCache.getApkFile(this, appUpdateStatus.latestUpdate)

        try {
            val installResult = appInstaller.startInstallation(this, file)
            val certificateHash = installResult.certificateHash ?: "error"
            displayAppInstallationSuccess(certificateHash)
            return true
        } catch (e: InstallationFailedException) {
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            displayAppInstallationFailure(e.translatedMessage, ex)
        } finally {
            // hide existing background notification for this app
            notificationRemover.removeAppStatusNotifications(this, app)
            hide(R.id.installingApplication)
        }
        return false
    }

    @MainThread
    private fun displayAppInstallationSuccess(certificateHash: String) {
        show(R.id.installerSuccess)
        show(R.id.fingerprintInstalledGood)
        setText(R.id.fingerprintInstalledGoodHash, certificateHash)
        if (!foregroundSettings.isDeleteUpdateIfInstallSuccessful) {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
    }

    @MainThread
    private fun displayAppInstallationFailure(errorMessage: String, exception: Exception) {
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
        if (!foregroundSettings.isDeleteUpdateIfInstallFailed) {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
    }

    @MainThread
    private fun displayDownloadFailure(description: String, exception: Exception?) {
        val downloadUrl = appUpdateStatus.latestUpdate.downloadUrl
        show(R.id.install_activity__download_file_failed)
        setText(R.id.install_activity__download_file_failed__url, downloadUrl)
        setText(R.id.install_activity__download_file_failed__text, description)
        if (exception != null) {
            val text = findViewById<TextView>(R.id.install_activity__download_file_failed__show_exception)
            text.setOnClickListener {
                val intent = CrashReportActivity.createIntent(this, exception, description)
                startActivity(intent)
            }
        }
    }

    @MainThread
    private fun displayFetchFailure(message: String, exception: Exception? = null) {
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