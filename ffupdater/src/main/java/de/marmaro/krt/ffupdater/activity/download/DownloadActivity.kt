package de.marmaro.krt.ffupdater.activity.download

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
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
import de.marmaro.krt.ffupdater.R.string.application_installation_was_not_successful
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__download_activity_fetching_url
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__download_activity_install_file
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
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
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
import de.marmaro.krt.ffupdater.storage.SystemFileManager
import de.marmaro.krt.ffupdater.utils.ifFalse
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

        val appFromExtras = intent.extras?.getString(EXTRA_APP_NAME)
        if (appFromExtras == null) {
            // InstallActivity was unintentionally started again after finishing the download
            finish()
            return
        }
        app = App.valueOf(appFromExtras)
        appImpl = app.findImpl()
        appInstaller = createForegroundAppInstaller(this, app)
        lifecycle.addObserver(appInstaller)

        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            deleteFileCache()
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            SystemFileManager.openFolder(appImpl.getApkCacheFolder(applicationContext), this)
        }

        // hide existing background notification for this app
        NotificationRemover.removeAppStatusNotifications(applicationContext, app)

        // prevent network timeouts when the displayed is automatically turned off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }
    }

    private fun deleteFileCache() {
        lifecycleScope.launch(Dispatchers.Main) {
            app.findImpl().deleteFileCache(applicationContext)
            hide(R.id.install_activity__delete_cache)
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isChangingConfigurations) {
            // if the device is not rotated, delete information about the download to allow a new download
            // next time
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
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun show(viewId: Int) {
        findViewById<View>(viewId).visibility = View.VISIBLE
    }

    private fun hide(viewId: Int) {
        findViewById<View>(viewId).visibility = View.GONE
    }

    private suspend fun <T> showViewDuringExecution(viewId: Int, block: suspend () -> T): T {
        findViewById<View>(viewId).visibility = View.VISIBLE
        try {
            return block()
        } finally {
            findViewById<View>(viewId).visibility = View.GONE
        }
    }

    private suspend fun <T> showViewAfterExecution(viewId: Int, block: suspend () -> T): T {
        try {
            return block()
        } finally {
            findViewById<View>(viewId).visibility = View.VISIBLE
        }
    }

    private fun setText(textId: Int, text: String) {
        findViewById<TextView>(textId).text = text
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

    private suspend fun executeDownloadProcess(status: InstalledAppStatus): Boolean {
        if (downloadViewModel.isDownloadForCurrentAppRunning(status)) {
            return reuseCurrentDownload(status)
        }

        val appImpl = app.findImpl()
        if (appImpl.isApkDownloaded(applicationContext, status.latestVersion)) {
            Log.d(LOG_TAG, "DownloadActivity: Use APK cache of ${app.name}.")
            show(R.id.useCachedDownloadedApk)
            val file = appImpl.getApkFile(applicationContext, status.latestVersion)
            setText(R.id.useCachedDownloadedApk__path, file.absolutePath)
            return true
        }

        isNetworkSuitable().ifFalse { return false }

        return startDownload(status)
    }

    private fun isStorageMounted(): Boolean {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            return true
        }
        show(R.id.externalStorageNotAccessible)
        setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
        return false
    }

    private fun isNetworkSuitable(): Boolean {
        if (ForegroundSettings.isDownloadOnMeteredAllowed || !isNetworkMetered(applicationContext)) {
            return true
        }
        displayFetchFailure(getString(main_activity__no_unmetered_network))
        return false
    }

    private fun showWarningIfNotEnoughStorageIsAvailable() {
        if (StorageUtil.isEnoughStorageAvailable(applicationContext)) {
            return
        }
        show(R.id.tooLowMemory)
        val mbs = StorageUtil.getFreeStorageInMebibytes(applicationContext)
        val message = getString(download_activity__too_low_memory_description, mbs)
        setText(R.id.tooLowMemoryDescription, message)
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
            displayFetchFailure(text, e)
            null
        }
    }

    private suspend fun fetchDownloadInformationWithoutErrorChecking(): InstalledAppStatus {
        val source = appImpl.downloadSource
        val inProgressText = getString(download_activity__fetch_url_for_download, source)
        val finishedText = getString(download_activity__fetched_url_for_download_successfully, source)

        setText(R.id.fetchUrlTextView, inProgressText)
        val status = showViewDuringExecution(R.id.fetchUrl) {
            appImpl.findInstalledAppStatus(applicationContext, USE_EVEN_OUTDATED_CACHE)
        }

        show(R.id.fetchedUrlSuccess)
        setText(R.id.fetchedUrlSuccessTextView, finishedText)
        return status
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
            displayDownloadFailure(status, text, e)
            false
        }
    }

    @MainThread
    private suspend fun startDownloadWithoutErrorChecking(status: InstalledAppStatus): Boolean {
        Log.d(LOG_TAG, "DownloadActivity: Start download of ${app.name}.")
        setText(R.id.downloadingFileUrl, status.latestVersion.downloadUrl)
        setText(R.id.downloadedFileUrl, status.latestVersion.downloadUrl)
        setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))
        showViewDuringExecution(R.id.downloadingFile) {
            showViewAfterExecution(R.id.downloadedFile) {
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
                showDownloadProgress(progressChannel)
            }
        }
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
    private suspend fun reuseCurrentDownload(status: InstalledAppStatus): Boolean {
        Log.d(LOG_TAG, "DownloadActivity: Reuse running download of ${app.name}.")
        show(R.id.downloadingFile)
        setText(R.id.downloadingFileUrl, status.latestVersion.downloadUrl)
        setText(R.id.downloadingFileText, getString(download_activity__download_app_with_status))

        showDownloadProgress(downloadViewModel.progressChannel!!)

        try {
            // NPE was thrown in #359 - it should be safe to ignore null values
            downloadViewModel.deferred?.await()
            return true
        } catch (e: Exception) {
            val text = when (e) {
                is NetworkException -> getString(install_activity__download_file_failed__crash_text)
                is DisplayableException -> e.message ?: e.javaClass.name
                else -> throw e
            }
            displayDownloadFailure(status, text, e)
        } finally {
            hide(R.id.downloadingFile)
        }
        Log.d(LOG_TAG, "DownloadActivity: Reusing failed for ${app.name}.")
        return false
    }

    @MainThread
    private suspend fun installApp(status: InstalledAppStatus): Boolean {
        Log.d(LOG_TAG, "DownloadActivity: Install app ${app.name}.")
        show(R.id.installingApplication)
        val file = appImpl.getApkFile(applicationContext, status.latestVersion)

        try {
            val installResult = appInstaller.startInstallation(this@DownloadActivity, file)
            val certificateHash = installResult.certificateHash ?: "error"
            displayAppInstallationSuccess(certificateHash)
            return true
        } catch (e: InstallationFailedException) {
            val ex = RuntimeException("Failed to install ${app.name} in the foreground.", e)
            displayAppInstallationFailure(e.translatedMessage, ex)
        } finally {
            // hide existing background notification for applicationContext app
            NotificationRemover.removeAppStatusNotifications(applicationContext, app)
            hide(R.id.installingApplication)
        }
        return false
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
    private fun displayDownloadFailure(status: InstalledAppStatus, description: String, exception: Exception?) {
        show(R.id.install_activity__download_file_failed)
        setText(R.id.install_activity__download_file_failed__url, status.latestVersion.downloadUrl)
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