package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUriExposedException
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.R.string.application_installation_was_not_successful
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__download_activity_fetching_url
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__download_activity_install_file
import de.marmaro.krt.ffupdater.R.string.download_activity__download_app_with_status
import de.marmaro.krt.ffupdater.R.string.download_activity__fetch_url_for_download
import de.marmaro.krt.ffupdater.R.string.download_activity__fetched_url_for_download_successfully
import de.marmaro.krt.ffupdater.R.string.download_activity__file_uri_exposed_toast
import de.marmaro.krt.ffupdater.R.string.download_activity__github_rate_limit_exceeded
import de.marmaro.krt.ffupdater.R.string.download_activity__open_folder
import de.marmaro.krt.ffupdater.R.string.download_activity__temporary_network_issue
import de.marmaro.krt.ffupdater.R.string.download_activity__too_low_memory_description
import de.marmaro.krt.ffupdater.R.string.install_activity__download_file_failed__crash_text
import de.marmaro.krt.ffupdater.R.string.main_activity__no_unmetered_network
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.AppInstaller.Companion.createForegroundAppInstaller
import de.marmaro.krt.ffupdater.installer.entity.Installer.SESSION_INSTALLER
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.USE_EVEN_OUTDATED_CACHE
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.notification.NotificationRemover
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.storage.StorageUtil
import de.marmaro.krt.ffupdater.utils.MethodResult
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
    private lateinit var viewModel: InstallActivityViewModel
    private lateinit var app: App
    private lateinit var appImpl: AppBase
    private lateinit var installedAppStatus: InstalledAppStatus
    private lateinit var appInstaller: AppInstaller

    // persistent data for already running downloads
    class InstallActivityViewModel : ViewModel() {
        private var downloadApp: App? = null
        var downloadDeferred: Deferred<Any>? = null
        var downloadProgressChannel: Channel<DownloadStatus>? = null
        var installationSuccess: Boolean = false

        fun storeNewRunningDownload(
            app: App,
            deferred: Deferred<Any>,
            progressChannel: Channel<DownloadStatus>,
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
            installationSuccess = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
        if (appFromExtras == null) {
            // InstallActivity was unintentionally started again after finishing the download
            finish()
            return
        }
        app = App.valueOf(appFromExtras)
        appImpl = app.findImpl()

        viewModel = ViewModelProvider(this)[InstallActivityViewModel::class.java]
        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                app.findImpl().deleteFileCache(applicationContext)
                hide(R.id.install_activity__delete_cache)
            }
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            tryOpenDownloadFolderInFileManager()
        }

        // hide existing background notification for this app
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)

        // prevent network timeouts when the displayed is automatically turned off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }

        appInstaller = createForegroundAppInstaller(this, app)
        lifecycle.addObserver(appInstaller)
    }

    override fun onStop() {
        super.onStop()

        if (!isChangingConfigurations) {
            // if the device is not rotated, delete information about the download to allow a new download
            // next time
            if (viewModel.installationSuccess) {
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
            viewModel.clear()
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun tryOpenDownloadFolderInFileManager() {
        val absolutePath = appImpl.getApkCacheFolder(applicationContext).absolutePath
        val uri = Uri.parse("file://$absolutePath/")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        val chooser = Intent.createChooser(intent, getString(download_activity__open_folder))
        showBriefMessage(download_activity__file_uri_exposed_toast)
        if (DeviceSdkTester.supportsAndroidNougat()) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
            try {
                startActivity(chooser)
            } catch (e: FileUriExposedException) {
                showBriefMessage(download_activity__file_uri_exposed_toast)
            }
        } else {
            startActivity(chooser)
        }
    }

    @UiThread
    private fun showBriefMessage(message: Int) {
        val layout = findViewById<View>(R.id.download_activity__root_view)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
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
        Log.d(LOG_TAG, "startInstallationProcess(): Start process for ${app.name}.")

        checkIfStorageIsMounted()
            .onFailure { return@startInstallationProcess }

        checkIfEnoughStorageAvailable()

        executeDownloadProcess()
            .onFailure { return@startInstallationProcess }

        installApp()
            .onSuccess {
                appImpl.appWasInstalledCallback(applicationContext, installedAppStatus)
                viewModel.installationSuccess = true
            }
            .onFailure { viewModel.installationSuccess = false }
    }

    private suspend fun executeDownloadProcess(): MethodResult {
        fetchDownloadInformationOrUseCache()
            .onFailure { return@executeDownloadProcess it }

        if (viewModel.isDownloadForCurrentAppRunning(app)) {
            reuseCurrentDownload()
                .onFailure { return@executeDownloadProcess it }
            return MethodResult.success()
        }

        val appImpl = app.findImpl()
        val latestUpdate = installedAppStatus.latestVersion
        if (appImpl.isApkDownloaded(applicationContext, latestUpdate)) {
            Log.d(LOG_TAG, "DownloadActivity: Use APK cache of ${app.name}.")
            show(R.id.useCachedDownloadedApk)
            val file = appImpl.getApkFile(applicationContext, latestUpdate)
            setText(R.id.useCachedDownloadedApk__path, file.absolutePath)
            return MethodResult.success()
        }

        startDownload()
            .onFailure { return@executeDownloadProcess it }
        return MethodResult.success()
    }

    private fun checkIfStorageIsMounted(): MethodResult {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            show(R.id.externalStorageNotAccessible)
            setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
            return MethodResult.failure()
        }
        return MethodResult.success()
    }

    private fun checkIfEnoughStorageAvailable() {
        if (!StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            show(R.id.tooLowMemory)
            val mbs = StorageUtil.getFreeStorageInMebibytes(applicationContext)
            val message = getString(download_activity__too_low_memory_description, mbs)
            setText(R.id.tooLowMemoryDescription, message)
        }
    }

    private suspend fun fetchDownloadInformationOrUseCache(): MethodResult {
        show(R.id.fetchUrl)
        val downloadSource = appImpl.downloadSource
        setText(R.id.fetchUrlTextView, getString(download_activity__fetch_url_for_download, downloadSource))

        try {
            installedAppStatus = appImpl.findInstalledAppStatus(applicationContext, USE_EVEN_OUTDATED_CACHE)
        } catch (e: Exception) {
            val text = when (e) {
                is ApiRateLimitExceededException -> getString(download_activity__github_rate_limit_exceeded)
                is DisplayableException -> getString(download_activity__temporary_network_issue)
                else -> throw e
            }
            displayFetchFailure(text, e)
            return MethodResult.failure()
        }

        hide(R.id.fetchUrl)
        show(R.id.fetchedUrlSuccess)
        val finishedText = getString(download_activity__fetched_url_for_download_successfully, downloadSource)
        setText(R.id.fetchedUrlSuccessTextView, finishedText)
        return MethodResult.success()
    }

    @MainThread
    private suspend fun startDownload(): MethodResult {
        Log.d(LOG_TAG, "startInstallationProcess(): Start download of ${app.name}.")
        if (!ForegroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(applicationContext)) {
            displayFetchFailure(getString(main_activity__no_unmetered_network))
            return MethodResult.failure()
        }

        val appImpl = app.findImpl()
        show(R.id.downloadingFile)
        setText(R.id.downloadingFileUrl, installedAppStatus.latestVersion.downloadUrl)
        setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))

        try {
            withContext(viewModel.viewModelScope.coroutineContext) {
                appImpl.download(applicationContext, installedAppStatus.latestVersion) { deferred, progressChannel ->
                    viewModel.storeNewRunningDownload(app, deferred, progressChannel)
                    showDownloadProgress(progressChannel)
                }
            }
            return MethodResult.success()
        } catch (e: Exception) {
            val text = when (e) {
                is NetworkException -> getString(install_activity__download_file_failed__crash_text)
                is DisplayableException -> e.message ?: e.javaClass.name
                else -> throw e
            }
            displayDownloadFailure(text, e)
        } finally {
            hide(R.id.downloadingFile)
            show(R.id.downloadedFile)
            setText(R.id.downloadedFileUrl, installedAppStatus.latestVersion.downloadUrl)
        }
        return MethodResult.failure()
    }

    private suspend fun showDownloadProgress(progressChannel: Channel<DownloadStatus>) {
        for (progress in progressChannel) {
            if (progress.progressInPercent != null) {
                findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = progress.progressInPercent
            }

            val text = when {
                progress.progressInPercent != null -> " (${progress.progressInPercent}%)"
                else -> " (${progress.totalMB}MB)"
            }
            setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status) + text)
        }
    }


    @MainThread
    private suspend fun reuseCurrentDownload(): MethodResult {
        Log.d(LOG_TAG, "reuseCurrentDownload(): Reuse running download of ${app.name}.")
        show(R.id.downloadingFile)
        val latestUpdate = installedAppStatus.latestVersion
        setText(R.id.downloadingFileUrl, latestUpdate.downloadUrl)
        setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))

        showDownloadProgress(viewModel.downloadProgressChannel!!)

        try {
            // NPE was thrown in #359 - it should be safe to ignore null values
            viewModel.downloadDeferred?.await()
            return MethodResult.success()
        } catch (e: Exception) {
            val text = when (e) {
                is NetworkException -> getString(install_activity__download_file_failed__crash_text)
                is DisplayableException -> e.message ?: e.javaClass.name
                else -> throw e
            }
            displayDownloadFailure(text, e)
        } finally {
            hide(R.id.downloadingFile)
        }
        Log.d(LOG_TAG, "reuseCurrentDownload(): Reusing failed for ${app.name}.")
        return MethodResult.failure()
    }

    @MainThread
    private suspend fun installApp(): MethodResult {
        Log.d(LOG_TAG, "Install app ${app.name}.")
        show(R.id.installingApplication)
        val file = appImpl.getApkFile(applicationContext, installedAppStatus.latestVersion)

        try {
            val installResult = appInstaller.startInstallation(this@DownloadActivity, file)
            val certificateHash = installResult.certificateHash ?: "error"
            displayAppInstallationSuccess(certificateHash)
            return MethodResult.success()
        } catch (e: InstallationFailedException) {
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            displayAppInstallationFailure(e.translatedMessage, ex)
        } finally {
            // hide existing background notification for applicationContext app
            NotificationRemover.removeAppStatusNotifications(applicationContext, app)
            hide(R.id.installingApplication)
        }
        return MethodResult.failure()
    }

    @MainThread
    private fun displayAppInstallationSuccess(certificateHash: String) {
        show(R.id.installerSuccess)
        show(R.id.fingerprintInstalledGood)
        setText(R.id.fingerprintInstalledGoodHash, certificateHash)
        if (!ForegroundSettings.isDeleteUpdateIfInstallSuccessful) {
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
        if (InstallerSettings.getInstallerMethod() == SESSION_INSTALLER) {
            show(R.id.install_activity__different_installer_info)
        }

        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        findViewById<TextView>(R.id.install_activity__exception__description).text = errorMessage
        findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = getString(crash_report__explain_text__download_activity_install_file)
            val intent = CrashReportActivity.createIntent(applicationContext, throwableAndLogs, description)
            startActivity(intent)
        }

        val cacheFolder = appImpl.getApkCacheFolder(applicationContext).absolutePath
        setText(R.id.install_activity__cache_folder_path, cacheFolder)
        if (!ForegroundSettings.isDeleteUpdateIfInstallFailed) {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
    }

    @MainThread
    private fun displayDownloadFailure(description: String, exception: Exception?) {
        val downloadUrl = installedAppStatus.latestVersion.downloadUrl
        show(R.id.install_activity__download_file_failed)
        setText(R.id.install_activity__download_file_failed__url, downloadUrl)
        setText(R.id.install_activity__download_file_failed__text, description)
        if (exception != null) {
            val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
            val text = findViewById<TextView>(R.id.install_activity__download_file_failed__show_exception)
            text.setOnClickListener {
                val intent = CrashReportActivity.createIntent(applicationContext, throwableAndLogs, description)
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
        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = getString(crash_report__explain_text__download_activity_fetching_url)
            val intent = CrashReportActivity.createIntent(applicationContext, throwableAndLogs, description)
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