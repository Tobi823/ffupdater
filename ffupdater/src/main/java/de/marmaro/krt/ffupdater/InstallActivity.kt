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
import de.marmaro.krt.ffupdater.R.string.*
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.background.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.download.AppCache
import de.marmaro.krt.ffupdater.download.FileDownloader
import de.marmaro.krt.ffupdater.download.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.download.StorageUtil
import de.marmaro.krt.ffupdater.installer.ForegroundAppInstaller
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper.Installer.SESSION_INSTALLER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Activity for downloading and installing apps on devices with API Level >= 24/Nougat.
 * Reason: If have to use the DownloadManager because this is the easiest way to download the app and access it with
 * the scheme format (for example: content://downloads/all_downloads/20).
 * The DownloadManager is more difficult to use then the default java way, but the DownloadManager offers more features
 * like restarting downloads, showing the current download status etc.
 */
class InstallActivity : AppCompatActivity() {
    private lateinit var viewModel: InstallActivityViewModel
    private lateinit var appInstaller: ForegroundAppInstaller
    private lateinit var appCache: AppCache
    private lateinit var foregroundSettings: ForegroundSettingsHelper
    private lateinit var installerSettingsHelper: InstallerSettingsHelper

    // persistent data across orientation changes
    class InstallActivityViewModel : ViewModel() {
        var app: App? = null
        var fileDownloader: FileDownloader? = null
        var updateCheckResult: UpdateCheckResult? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.install_activity)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this).get(InstallActivityViewModel::class.java)
        if (viewModel.app == null) {
            viewModel.app = intent.extras?.getString(EXTRA_APP_NAME)
                ?.let { App.valueOf(it) }
                ?: run {
                    // InstallActivity was unintentionally started again after finishing the download
                    finish()
                    return
                }
        }

        foregroundSettings = ForegroundSettingsHelper(this)
        installerSettingsHelper = InstallerSettingsHelper(this)
        appCache = AppCache(viewModel.app!!)
        appInstaller = ForegroundAppInstaller.create(this, viewModel.app!!, appCache.getFile(this))
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

        lifecycleScope.launch(Dispatchers.Main) {
            startInstallationProcess()
        }
    }

    private fun tryOpenDownloadFolderInFileManager() {
        val intent = Intent(Intent.ACTION_VIEW)
        val parentFolder = appCache.getFile(this).parentFile ?: return
        val uri = Uri.parse("file://${parentFolder.absolutePath}/")
        intent.setDataAndType(uri, "resource/folder")
        val chooser = Intent.createChooser(intent, getString(install_activity__open_folder))

        if (DeviceSdkTester.supportsAndroidNougat()) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
            try {
                startActivity(chooser)
            } catch (e: FileUriExposedException) {
                Toast.makeText(this, install_activity__file_uri_exposed_toast, LENGTH_LONG)
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
        failureExternalStorageNotAccessible()
    }

    private suspend fun checkIfEnoughStorageAvailable() {
        if (StorageUtil.isEnoughStorageAvailable(this)) {
            fetchDownloadInformation()
            return
        }
        show(R.id.tooLowMemory)
        val mbs = StorageUtil.getFreeStorageInMebibytes(this)
        val message = getString(install_activity__too_low_memory_description, mbs)
        setText(R.id.tooLowMemoryDescription, message)
        fetchDownloadInformation()
    }

    private suspend fun fetchDownloadInformation() {
        show(R.id.fetchUrl)
        val downloadSource = getString(viewModel.app!!.detail.displayDownloadSource)
        val runningText = getString(install_activity__fetch_url_for_download, downloadSource)
        setText(R.id.fetchUrlTextView, runningText)

        // check if network type requirements are met
        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            failureShowFetchUrlException(getString(main_activity__no_unmetered_network))
            return
        }

        val updateCheckResult = try {
            viewModel.app!!.detail.updateCheckAsync(this).await()
        } catch (e: GithubRateLimitExceededException) {
            failureShowFetchUrlException(getString(install_activity__github_rate_limit_exceeded), e)
            return
        } catch (e: NetworkException) {
            failureShowFetchUrlException(getString(install_activity__temporary_network_issue), e)
            return
        }

        viewModel.updateCheckResult = updateCheckResult
        hide(R.id.fetchUrl)
        show(R.id.fetchedUrlSuccess)
        val finishedText = getString(install_activity__fetched_url_for_download_successfully, downloadSource)
        setText(R.id.fetchedUrlSuccessTextView, finishedText)
        if (appCache.isAvailable(this, updateCheckResult.availableResult)) {
            show(R.id.useCachedDownloadedApk)
            setText(R.id.useCachedDownloadedApk__path, appCache.getFile(this).absolutePath)
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
            failureShowFetchUrlException(getString(main_activity__no_unmetered_network))
            return
        }

        val updateCheckResult = requireNotNull(viewModel.updateCheckResult)
        show(R.id.downloadingFile)
        setText(R.id.downloadingFileUrl, updateCheckResult.downloadUrl)

        val fileDownloader = FileDownloader()
        viewModel.fileDownloader = fileDownloader
        fileDownloader.onProgress = { percentage, mb ->
            runOnUiThread {
                val status = if (percentage != null) {
                    findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = percentage
                    "${percentage}%"
                } else {
                    "${mb}MB"
                }
                val display = getString(install_activity__download_app_with_status, status)
                setText(R.id.downloadingFileText, display)
            }
        }

        val display = getString(install_activity__download_app_with_status, "")
        setText(R.id.downloadingFileText, display)

        val url = updateCheckResult.availableResult.downloadUrl
        appCache.delete(this)
        val file = appCache.getFile(this)

        // this coroutine should survive a screen rotation and should live as long as the view model
        try {
            withContext(viewModel.viewModelScope.coroutineContext) {
                fileDownloader.downloadFileAsync(url, file).await()
            }
            hide(R.id.downloadingFile)
            show(R.id.downloadedFile)
            setText(R.id.downloadedFileUrl, updateCheckResult.downloadUrl)
            installApp()
        } catch (e: NetworkException) {
            hide(R.id.downloadingFile)
            show(R.id.downloadedFile)
            setText(R.id.downloadedFileUrl, updateCheckResult.downloadUrl)
            failureDownloadUnsuccessful(e)
        }
    }

    @MainThread
    private suspend fun reuseCurrentDownload() {
        val updateCheckResult = requireNotNull(viewModel.updateCheckResult)
        show(R.id.downloadingFile)
        setText(R.id.downloadingFileUrl, updateCheckResult.downloadUrl)
        val fileDownloader = requireNotNull(viewModel.fileDownloader)
        fileDownloader.onProgress = { percentage, mb ->
            runOnUiThread {
                val status = if (percentage != null) {
                    findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = percentage
                    "${percentage}%"
                } else {
                    "${mb}MB"
                }
                val display = getString(install_activity__download_app_with_status, status)
                setText(R.id.downloadingFileText, display)
            }
        }

        try {
            fileDownloader.currentDownload?.await()
            installApp()
        } catch (e: NetworkException) {
            failureDownloadUnsuccessful(e)
        }
    }

    @MainThread
    private suspend fun installApp() {
        show(R.id.installingApplication)
        val result = appInstaller.installAsync(this).await()

        // hide existing background notification for this app
        BackgroundNotificationBuilder.hideUpdateIsAvailable(this, viewModel.app!!)
        BackgroundNotificationBuilder.hideInstallationSuccess(this, viewModel.app!!)
        BackgroundNotificationBuilder.hideInstallationError(this, viewModel.app!!)

        if (result.success) {
            hide(R.id.installingApplication)
            show(R.id.installerSuccess)
            show(R.id.fingerprintInstalledGood)
            setText(R.id.fingerprintInstalledGoodHash, result.certificateHash ?: "/")

            val available = requireNotNull(viewModel.updateCheckResult).availableResult
            viewModel.app!!.detail.appIsInstalled(this, available)

            if (foregroundSettings.isDeleteUpdateIfInstallSuccessful) {
                appCache.delete(this)
            } else {
                show(R.id.install_activity__delete_cache)
            }
            return
        }

        hide(R.id.installingApplication)
        show(R.id.installerFailed)
        if (installerSettingsHelper.getInstaller() == SESSION_INSTALLER) {
            show(R.id.install_activity__different_installer_info)
        }

        show(R.id.install_activity__open_cache_folder)
        val cacheFolder = appCache.getFile(this).parentFile?.absolutePath ?: ""
        setText(R.id.install_activity__cache_folder_path, cacheFolder)
        setText(R.id.installerFailedReason, result.errorMessage ?: "/")

        if (foregroundSettings.isDeleteUpdateIfInstallFailed) {
            appCache.delete(this)
        } else {
            show(R.id.install_activity__delete_cache)
        }

        if (result.errorException != null) {
            findViewById<TextView>(R.id.installerFailedReason).setOnClickListener {
                val description = getString(crash_report___explain_text__install_activity_install_file)
                val intent = CrashReportActivity.createIntent(this, result.errorException, description)
                startActivity(intent)
            }
        }
    }

    @MainThread
    private fun failureExternalStorageNotAccessible() {
        show(R.id.externalStorageNotAccessible)
        setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
    }

    @MainThread
    private fun failureDownloadUnsuccessful(exception: Exception) {
        val updateCheckResult = requireNotNull(viewModel.updateCheckResult)
        hide(R.id.downloadingFile)
        show(R.id.downloadFileFailed)
        setText(R.id.downloadFileFailedUrl, updateCheckResult.downloadUrl)

        findViewById<TextView>(R.id.downloadFileFailedShowException).setOnClickListener {
            val description = getString(crash_report___explain_text__install_activity_download_file)
            val intent = CrashReportActivity.createIntent(this, exception, description)
            startActivity(intent)
        }
        show(R.id.installerFailed)
        appCache.delete(this)
    }

    @MainThread
    private fun failureShowFetchUrlException(message: String, exception: Exception? = null) {
        hide(R.id.fetchUrl)
        show(R.id.install_activity__exception)
        setText(R.id.install_activity__exception__text, message)
        if (exception == null) {
            hide(R.id.install_activity__exception__show_button)
            return
        }

        findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = getString(crash_report__explain_text__install_activity_fetching_url)
            val intent = CrashReportActivity.createIntent(this, exception, description)
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        fun createIntent(context: Context, app: App): Intent {
            val intent = Intent(context, InstallActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(EXTRA_APP_NAME, app.name)
            return intent
        }
    }
}