package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUriExposedException
import android.os.StrictMode
import android.util.Log
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
import de.marmaro.krt.ffupdater.InstallActivity.State.*
import de.marmaro.krt.ffupdater.R.id.install_activity__exception__show_button
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__install_activity_fetching_url
import de.marmaro.krt.ffupdater.R.string.install_activity__file_uri_exposed_toast
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.BuildMetadata
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.download.AppCache
import de.marmaro.krt.ffupdater.download.AppDownloadStatus
import de.marmaro.krt.ffupdater.download.FileDownloader
import de.marmaro.krt.ffupdater.download.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.download.StorageUtil
import de.marmaro.krt.ffupdater.installer.ForegroundAppInstaller
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    // necessary for communication with State enums
    private lateinit var app: App
    private var state = SUCCESS_PAUSE
    private var stateJob: Job? = null

    // persistent data across orientation changes
    class InstallActivityViewModel : ViewModel() {
        var app: App? = null
        var fileDownloader: FileDownloader? = null
        var updateCheckResult: UpdateCheckResult? = null
        var error: Pair<Int?, Exception?>? = null
        var installationError: Pair<Int, String>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.install_activity)
        CrashListener.openCrashReporterForUncaughtExceptions(this)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val passedAppName = intent.extras?.getString(EXTRA_APP_NAME)
        if (passedAppName == null) {
            // InstallActivity was unintentionally started again after finishing the download
            finish()
            return
        }
        app = App.valueOf(passedAppName)

        foregroundSettings = ForegroundSettingsHelper(this)
        appCache = AppCache(app)
        appInstaller = ForegroundAppInstaller.create(this, app, appCache.getFile(this))
        lifecycle.addObserver(appInstaller)

        findViewById<Button>(R.id.install_activity__delete_cache_button).setOnClickListener {
            appCache.delete(this)
            hide(R.id.install_activity__delete_cache)
        }
        findViewById<Button>(R.id.install_activity__open_cache_folder_button).setOnClickListener {
            tryOpenDownloadFolderInFileManager()
        }

        // make sure that the ViewModel is correct for the current app
        viewModel = ViewModelProvider(this).get(InstallActivityViewModel::class.java)
        if (viewModel.app != null) {
            check(viewModel.app == app)
        }
        viewModel.app = app
        // only start new download if no download is still running (can happen after rotation)
        restartStateMachine(START)
    }

    private fun tryOpenDownloadFolderInFileManager() {
        val intent = Intent(Intent.ACTION_VIEW)
        val parentFolder = appCache.getFile(this).parentFile ?: return
        val uri = Uri.parse("file://${parentFolder.absolutePath}/")
        intent.setDataAndType(uri, "resource/folder")
        val chooser = Intent.createChooser(intent, "Open folder")

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

    override fun onStop() {
        super.onStop()
        // finish activity when it's finished and the user switch to another app
        if (state == SUCCESS_STOP || state == ERROR_STOP) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun restartStateMachine(jumpDestination: State) {
        // security check to prevent a illegal restart
        if (state != SUCCESS_PAUSE) {
            return
        }
        stateJob?.cancel()
        state = jumpDestination
        stateJob = lifecycleScope.launch(Dispatchers.Main) {
            while (state != ERROR_STOP
                && state != SUCCESS_PAUSE
                && state != SUCCESS_STOP
            ) {
                state = state.action(this@InstallActivity)
            }
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

    @MainThread
    enum class State(val action: suspend (InstallActivity) -> State) {
        START(InstallActivity::start),
        CHECK_IF_STORAGE_IS_MOUNTED(InstallActivity::checkIfStorageIsMounted),
        CHECK_FOR_ENOUGH_STORAGE(InstallActivity::checkForEnoughStorage),
        FETCH_DOWNLOAD_INFORMATION(InstallActivity::fetchDownloadInformation),
        START_DOWNLOAD(InstallActivity::startDownload),
        REUSE_CURRENT_DOWNLOAD(InstallActivity::reuseCurrentDownload),
        INSTALL_APP(InstallActivity::installApp),

        //===============================================

        FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE(InstallActivity::failureExternalStorageNotAccessible),
        FAILURE_DOWNLOAD_UNSUCCESSFUL(InstallActivity::failureDownloadUnsuccessful),
        FAILURE_SHOW_FETCH_URL_EXCEPTION(InstallActivity::failureShowFetchUrlException),

        // SUCCESS_PAUSE => state machine will be restarted externally
        SUCCESS_PAUSE({ SUCCESS_PAUSE }),
        SUCCESS_STOP({ SUCCESS_STOP }),
        ERROR_STOP({ ERROR_STOP });
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"

        @MainThread
        fun start(ia: InstallActivity): State {
            return CHECK_IF_STORAGE_IS_MOUNTED
        }

        @MainThread
        fun checkIfStorageIsMounted(ia: InstallActivity): State {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                return CHECK_FOR_ENOUGH_STORAGE
            }
            return FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE
        }

        @MainThread
        fun checkForEnoughStorage(ia: InstallActivity): State {
            if (StorageUtil.isEnoughStorageAvailable()) {
                return FETCH_DOWNLOAD_INFORMATION
            }
            ia.show(R.id.tooLowMemory)
            val mbs = StorageUtil.getFreeStorageInMebibytes()
            val message = ia.getString(R.string.install_activity__too_low_memory_description, mbs)
            ia.setText(R.id.tooLowMemoryDescription, message)
            return FETCH_DOWNLOAD_INFORMATION
        }

        @MainThread
        suspend fun fetchDownloadInformation(ia: InstallActivity): State {
            val app = ia.app
            ia.show(R.id.fetchUrl)
            val downloadSource = ia.getString(app.detail.displayDownloadSource)
            val runningText = ia.getString(
                R.string.install_activity__fetch_url_for_download,
                downloadSource
            )
            ia.setText(R.id.fetchUrlTextView, runningText)

            // check if network type requirements are met
            if (!ia.foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(ia)) {
                ia.viewModel.error = Pair(R.string.main_activity__no_unmetered_network, null)
                return FAILURE_SHOW_FETCH_URL_EXCEPTION
            }

            val updateCheckResult = try {
                app.detail.updateCheck(ia)
            } catch (e: GithubRateLimitExceededException) {
                ia.viewModel.error = Pair(R.string.install_activity__github_rate_limit_exceeded, e)
                return FAILURE_SHOW_FETCH_URL_EXCEPTION
            } catch (e: NetworkException) {
                ia.viewModel.error = Pair(R.string.install_activity__temporary_network_issue, e)
                return FAILURE_SHOW_FETCH_URL_EXCEPTION
            }

            ia.viewModel.updateCheckResult = updateCheckResult
            ia.hide(R.id.fetchUrl)
            ia.show(R.id.fetchedUrlSuccess)
            val finishedText = ia.getString(
                R.string.install_activity__fetched_url_for_download_successfully,
                downloadSource
            )
            ia.setText(R.id.fetchedUrlSuccessTextView, finishedText)
            if (ia.appCache.isAvailable(ia, updateCheckResult.availableResult)) {
                ia.show(R.id.useCachedDownloadedApk)
                ia.setText(R.id.useCachedDownloadedApk__path, ia.appCache.getFile(ia).absolutePath)
                return INSTALL_APP
            }

            if (ia.viewModel.fileDownloader?.currentDownloadResult != null) {
                return REUSE_CURRENT_DOWNLOAD
            }
            return START_DOWNLOAD
        }

        @MainThread
        suspend fun startDownload(ia: InstallActivity): State {
            if (!ia.foregroundSettings.isDownloadOnMeteredAllowed && isNetworkMetered(ia)) {
                ia.viewModel.error = Pair(R.string.main_activity__no_unmetered_network, null)
                return FAILURE_SHOW_FETCH_URL_EXCEPTION
            }

            val updateCheckResult = requireNotNull(ia.viewModel.updateCheckResult)
            ia.show(R.id.downloadingFile)
            ia.setText(R.id.downloadingFileUrl, updateCheckResult.downloadUrl)

            val fileDownloader = FileDownloader()
            ia.viewModel.fileDownloader = fileDownloader
            fileDownloader.onProgress = { percentage, mb ->
                ia.runOnUiThread {
                    val status = if (percentage != null) {
                        ia.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = percentage
                        "$percentage %"
                    } else {
                        "$mb MB"
                    }
                    val display = ia.getString(R.string.install_activity__download_app_with_status, status)
                    ia.setText(R.id.downloadingFileText, display)
                }
            }

            val display = ia.getString(R.string.install_activity__download_app_with_status, "")
            ia.setText(R.id.downloadingFileText, display)

            val url = updateCheckResult.availableResult.downloadUrl
            ia.appCache.delete(ia)
            val file = ia.appCache.getFile(ia)

            AppDownloadStatus.foregroundDownloadIsStarted()
            // download coroutine should survive a screen rotation and should live as long as the view model
            val result = withContext(ia.viewModel.viewModelScope.coroutineContext) {
                fileDownloader.downloadFileAsync(url, file).await()
            }
            AppDownloadStatus.foregroundDownloadIsFinished()

            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadedFile)
            ia.setText(R.id.downloadedFileUrl, updateCheckResult.downloadUrl)
            if (result) {
                return INSTALL_APP
            }
            return FAILURE_DOWNLOAD_UNSUCCESSFUL
        }

        @MainThread
        suspend fun reuseCurrentDownload(ia: InstallActivity): State {
            val updateCheckResult = requireNotNull(ia.viewModel.updateCheckResult)
            ia.show(R.id.downloadingFile)
            ia.setText(R.id.downloadingFileUrl, updateCheckResult.downloadUrl)
            val fileDownloader = requireNotNull(ia.viewModel.fileDownloader)
            fileDownloader.onProgress = { percentage, mb ->
                ia.runOnUiThread {
                    val status = if (percentage != null) {
                        ia.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = percentage
                        "$percentage %"
                    } else {
                        "$mb MB"
                    }
                    val display = ia.getString(R.string.install_activity__download_app_with_status, status)
                    ia.setText(R.id.downloadingFileText, display)
                }
            }

            val success = fileDownloader.currentDownloadResult?.await() ?: false
            AppDownloadStatus.foregroundDownloadIsFinished()
            if (success) {
                return INSTALL_APP
            }
            return FAILURE_DOWNLOAD_UNSUCCESSFUL
        }

        @MainThread
        suspend fun installApp(ia: InstallActivity): State {
            ia.show(R.id.installingApplication)
            val result = ia.appInstaller.installAsync().await()
            if (result.success) {
                ia.hide(R.id.installingApplication)
                ia.show(R.id.installerSuccess)
                ia.show(R.id.fingerprintInstalledGood)
                ia.setText(R.id.fingerprintInstalledGoodHash, result.certificateHash ?: "/")

                val updateCheckResult = requireNotNull(ia.viewModel.updateCheckResult)
                val available = updateCheckResult.availableResult
                ia.app.detail.appInstallationCallback(ia, available)
                if (BuildMetadata.isDebugBuild()) {
                    Log.i("InstallActivity", "Don't delete file to speedup local development.")
                    ia.show(R.id.install_activity__open_cache_folder)
                } else {
                    ia.appCache.delete(ia)
                }
                return SUCCESS_STOP
            }

            ia.viewModel.installationError = Pair(result.errorCode ?: -80, result.errorMessage ?: "/")
            ia.hide(R.id.installingApplication)
            ia.show(R.id.installerFailed)
            ia.show(R.id.install_activity__delete_cache)
            ia.show(R.id.install_activity__open_cache_folder)
            val cacheFolder = ia.appCache.getFile(ia).parentFile?.absolutePath ?: ""
            ia.setText(R.id.install_activity__cache_folder_path, cacheFolder)
            var error = ia.viewModel.installationError?.second
            if (error != null) {
                if ("INSTALL_FAILED_INTERNAL_ERROR" in error && "Permission Denied" in error) {
                    error += "\n\n${ia.getString(R.string.install_activity__try_disable_miui_optimization)}"
                }
                ia.setText(R.id.installerFailedReason, error)
            }
            return ERROR_STOP
        }

        //===============================================

        @MainThread
        fun failureExternalStorageNotAccessible(ia: InstallActivity): State {
            ia.show(R.id.externalStorageNotAccessible)
            ia.setText(
                R.id.externalStorageNotAccessible_state,
                Environment.getExternalStorageState()
            )
            return ERROR_STOP
        }

        @MainThread
        fun failureDownloadUnsuccessful(ia: InstallActivity): State {
            val updateCheckResult = requireNotNull(ia.viewModel.updateCheckResult)
            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadFileFailed)
            ia.setText(R.id.downloadFileFailedUrl, updateCheckResult.downloadUrl)
            ia.setText(R.id.downloadFileFailedText, ia.viewModel.fileDownloader?.errorMessage ?: "")
            ia.show(R.id.installerFailed)
            if (BuildMetadata.isDebugBuild()) {
                Log.i("InstallActivity", "Don't delete file to speedup local development.")
            } else {
                ia.appCache.delete(ia)
            }
            return ERROR_STOP
        }

        @MainThread
        fun failureShowFetchUrlException(ia: InstallActivity): State {
            ia.hide(R.id.fetchUrl)
            ia.show(R.id.install_activity__exception)
            val text = ia.viewModel.error?.first?.let { ia.getString(it) } ?: "/"
            ia.setText(R.id.install_activity__exception__text, text)
            val exception = ia.viewModel.error?.second
            if (exception == null) {
                ia.hide(install_activity__exception__show_button)
            } else {
                ia.findViewById<TextView>(install_activity__exception__show_button).setOnClickListener {
                    val description = ia.getString(crash_report__explain_text__install_activity_fetching_url)
                    val intent = CrashReportActivity.createIntent(ia, exception, description)
                    ia.startActivity(intent)
                }
            }
            return ERROR_STOP
        }

        fun createIntent(context: Context, app: App): Intent {
            val intent = Intent(context, InstallActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(EXTRA_APP_NAME, app.name)
            return intent
        }
    }
}