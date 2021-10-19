package de.marmaro.krt.ffupdater

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import de.marmaro.krt.ffupdater.InstallActivity.State.*
import de.marmaro.krt.ffupdater.R.id.install_activity__exception__show_button
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__install_activity_fetching_url
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiConsumerException
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.download.ApkCache
import de.marmaro.krt.ffupdater.download.DownloadManagerUtil
import de.marmaro.krt.ffupdater.download.DownloadManagerUtil.DownloadStatus.Status.*
import de.marmaro.krt.ffupdater.download.StorageUtil
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.FingerprintValidator.CertificateValidationResult
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.*


/**
 * Activity for downloading and installing apps on devices with API Level >= 24/Nougat.
 * Reason: If have to use the DownloadManager because this is the easiest way to download the app and access it with
 * the scheme format (for example: content://downloads/all_downloads/20).
 * The DownloadManager is more difficult to use then the default java way, but the DownloadManager offers more features
 * like restarting downloads, showing the current download status etc.
 */
class InstallActivity : AppCompatActivity() {
    private lateinit var viewModel: InstallActivityViewModel
    private lateinit var downloadManager: DownloadManager
    private lateinit var fingerprintValidator: FingerprintValidator
    private lateinit var appInstaller: AppInstaller
    private lateinit var apkCache: ApkCache

    // necessary for communication with State enums
    private lateinit var app: App
    private var state = SUCCESS_PAUSE
    private var stateJob: Job? = null
    private lateinit var fileFingerprint: CertificateValidationResult
    private lateinit var appFingerprint: CertificateValidationResult
    private var appInstallationFailedErrorMessage: String? = null

    // persistent data across orientation changes
    class InstallActivityViewModel : ViewModel() {
        var app: App? = null
        var downloadId: Long? = null
        var updateCheckResult: UpdateCheckResult? = null
        var fetchUrlException: Exception? = null
        var fetchUrlExceptionText: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.install_activity)
        CrashListener.openCrashReporterForUncaughtExceptions(this)
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val passedAppName = intent.extras?.getString(EXTRA_APP_NAME)
        if (passedAppName == null) {
            //InstallActivity was unintentionally started again after finishing the download
            finish()
            return
        }
        app = App.valueOf(passedAppName)

        fingerprintValidator = FingerprintValidator(packageManager)
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        appInstaller = AppInstaller.create(
            successfulInstallationCallback = {
                restartStateMachine(USER_HAS_INSTALLED_APP_SUCCESSFUL)
            },
            failedInstallationCallback = { errorMessage ->
                if (errorMessage != null) {
                    appInstallationFailedErrorMessage = errorMessage
                }
                restartStateMachine(FAILURE_APP_INSTALLATION)
            })
        apkCache = ApkCache(app, this)
        findViewById<View>(R.id.install_activity__retrigger_installation__button).setOnClickListener {
            restartStateMachine(TRIGGER_INSTALLATION_PROCESS)
        }

        //make sure that the ViewModel is correct for the current app
        viewModel = ViewModelProvider(this).get(InstallActivityViewModel::class.java)
        if (viewModel.app != null) {
            check(viewModel.app == app)
        }
        viewModel.app = app
        //recover from an orientation change - is the download already running/finished?
        if (viewModel.downloadId != null) {
            restartStateMachine(DOWNLOAD_IS_ENQUEUED)
            return
        }
        restartStateMachine(START)
    }

    override fun onStop() {
        super.onStop()
        //finish activity when it's finished and the user switch to another app
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        appInstaller.onActivityResult(requestCode, resultCode, data)
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

    /**
     * This method will be called when the app installation is completed.
     * @param intent intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        appInstaller.onNewIntentCallback(intent, this)
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
        INSTALLED_APP_SIGNATURE_CHECKED(InstallActivity::installedAppSignatureChecked),
        EXTERNAL_STORAGE_IS_ACCESSIBLE(InstallActivity::externalStorageIsAccessible),
        DOWNLOAD_MANAGER_IS_ENABLED(InstallActivity::downloadManagerIsEnabled),
        PRECONDITIONS_ARE_CHECKED(InstallActivity::preconditionsAreChecked),
        ENQUEUING_DOWNLOAD(InstallActivity::enqueuingDownload),
        DOWNLOAD_IS_ENQUEUED(InstallActivity::downloadIsEnqueued),
        DOWNLOAD_WAS_SUCCESSFUL(InstallActivity::downloadWasSuccessful),
        USE_CACHED_DOWNLOADED_APK(InstallActivity::useCachedDownloadedApk),
        FINGERPRINT_OF_DOWNLOADED_FILE_OK(InstallActivity::fingerprintOfDownloadedFileOk),
        TRIGGER_INSTALLATION_PROCESS(InstallActivity::triggerInstallationProcess),
        USER_HAS_INSTALLED_APP_SUCCESSFUL(InstallActivity::userHasInstalledAppSuccessful),
        APP_INSTALLATION_HAS_BEEN_REGISTERED(InstallActivity::appInstallationHasBeenRegistered),
        FINGERPRINT_OF_INSTALLED_APP_OK(InstallActivity::fingerprintOfInstalledAppOk),

        //===============================================

        FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP(InstallActivity::failureUnknownSignatureOfInstalledApp),
        FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE(InstallActivity::failureExternalStorageNotAccessible),
        FAILURE_DOWNLOAD_MANAGER_DISABLED(InstallActivity::failureDownloadManagerDisabled),
        FAILURE_DOWNLOAD_UNSUCCESSFUL(InstallActivity::failureDownloadUnsuccessful),
        FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE(InstallActivity::failureInvalidFingerprintOfDownloadedFile),
        FAILURE_APP_INSTALLATION(InstallActivity::failureAppInstallation),
        FAILURE_FINGERPRINT_OF_INSTALLED_APP_INVALID(InstallActivity::failureFingerprintOfInstalledAppInvalid),
        FAILURE_SHOW_FETCH_URL_EXCEPTION(InstallActivity::failureShowFetchUrlException),

        // SUCCESS_PAUSE => state machine will be restarted externally
        SUCCESS_PAUSE({ SUCCESS_PAUSE }),
        SUCCESS_STOP({ SUCCESS_STOP }),
        ERROR_STOP({ ERROR_STOP });
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"

        @MainThread
        suspend fun start(ia: InstallActivity): State {
            if (!ia.app.detail.isInstalled(ia)) {
                return INSTALLED_APP_SIGNATURE_CHECKED
            }
            if (ia.fingerprintValidator.checkInstalledApp(ia.app).isValid) {
                return INSTALLED_APP_SIGNATURE_CHECKED
            }
            return FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP
        }

        @MainThread
        fun installedAppSignatureChecked(ia: InstallActivity): State {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                return EXTERNAL_STORAGE_IS_ACCESSIBLE
            }
            return FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE
        }

        @MainThread
        fun externalStorageIsAccessible(ia: InstallActivity): State {
            val downloadManager = "com.android.providers.downloads"
            return try {
                if (ia.packageManager.getApplicationInfo(downloadManager, 0).enabled) {
                    DOWNLOAD_MANAGER_IS_ENABLED
                } else {
                    FAILURE_DOWNLOAD_MANAGER_DISABLED
                }
            } catch (e: PackageManager.NameNotFoundException) {
                FAILURE_DOWNLOAD_MANAGER_DISABLED
            }
        }

        @MainThread
        fun downloadManagerIsEnabled(ia: InstallActivity): State {
            if (StorageUtil.isEnoughStorageAvailable()) {
                return PRECONDITIONS_ARE_CHECKED
            }
            ia.show(R.id.tooLowMemory)
            val mbs = StorageUtil.getFreeStorageInMebibytes()
            val message = ia.getString(R.string.install_activity__too_low_memory_description, mbs)
            ia.setText(R.id.tooLowMemoryDescription, message)
            return PRECONDITIONS_ARE_CHECKED
        }

        @MainThread
        suspend fun preconditionsAreChecked(ia: InstallActivity): State {
            val app = ia.app
            ia.show(R.id.fetchUrl)
            val downloadSource = ia.getString(app.detail.displayDownloadSource)
            val runningText = ia.getString(
                R.string.install_activity__fetch_url_for_download,
                downloadSource
            )
            ia.setText(R.id.fetchUrlTextView, runningText)

            val updateCheckResult = try {
                app.detail.updateCheck(ia)
            } catch (e: GithubRateLimitExceededException) {
                ia.viewModel.fetchUrlException = e
                ia.viewModel.fetchUrlExceptionText =
                    ia.getString(R.string.install_activity__github_rate_limit_exceeded)
                return FAILURE_SHOW_FETCH_URL_EXCEPTION
            } catch (e: ApiConsumerException) {
                ia.viewModel.fetchUrlException = e
                ia.viewModel.fetchUrlExceptionText =
                    ia.getString(R.string.install_activity__temporary_network_issue)
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
            if (ia.apkCache.isCacheAvailable(updateCheckResult.availableResult)) {
                return USE_CACHED_DOWNLOADED_APK
            }
            return ENQUEUING_DOWNLOAD
        }

        @MainThread
        fun enqueuingDownload(ia: InstallActivity): State {
            ia.show(R.id.downloadingFile)
            val updateCheckResult = ia.viewModel.updateCheckResult!!
            ia.setText(R.id.downloadingFileUrl, updateCheckResult.downloadUrl)
            ia.viewModel.downloadId = DownloadManagerUtil.enqueue(
                downloadManager = ia.downloadManager,
                context = ia,
                app = ia.app,
                availableVersionResult = updateCheckResult.availableResult
            )
            return DOWNLOAD_IS_ENQUEUED
        }

        @MainThread
        suspend fun downloadIsEnqueued(ia: InstallActivity): State {
            do {
                val downloadStatus = DownloadManagerUtil.getStatusAndProgress(
                    ia.downloadManager,
                    ia.viewModel.downloadId!!
                )
                val downloadStatusText = DownloadManagerUtil.getStatusText(ia, downloadStatus)

                ia.setText(
                    R.id.downloadingFileText,
                    ia.getString(
                        R.string.install_activity__download_application_from_with_status,
                        downloadStatusText
                    )
                )
                ia.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress =
                    downloadStatus.progressInPercentage

                if (downloadStatus.status == SUCCESSFUL) {
                    return DOWNLOAD_WAS_SUCCESSFUL
                }
                delay(500L)
            } while (downloadStatus.status != FAILED)
            return FAILURE_DOWNLOAD_UNSUCCESSFUL
        }

        @MainThread
        suspend fun downloadWasSuccessful(ia: InstallActivity): State {
            ia.show(R.id.downloadedFile)
            val app = ia.app
            ia.hide(R.id.downloadingFile)
            ia.setText(R.id.downloadedFileUrl, ia.viewModel.updateCheckResult!!.downloadUrl)
            ia.show(R.id.verifyDownloadFingerprint)

            ia.apkCache.moveDownloadToCache(ia.downloadManager, ia.viewModel.downloadId!!)
            val fingerprint = ia.fingerprintValidator.checkApkFile(ia.apkCache.getCacheFile(), app)
            ia.fileFingerprint = fingerprint
            return if (fingerprint.isValid) {
                FINGERPRINT_OF_DOWNLOADED_FILE_OK
            } else {
                ia.apkCache.deleteCache()
                FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE
            }
        }

        @MainThread
        suspend fun useCachedDownloadedApk(ia: InstallActivity): State {
            val app = ia.app
            ia.show(R.id.useCachedDownloadedApk)
            ia.setText(R.id.useCachedDownloadedApk__path, ia.apkCache.getCacheFile().absolutePath)
            ia.show(R.id.verifyDownloadFingerprint)

            val fingerprint = ia.fingerprintValidator.checkApkFile(ia.apkCache.getCacheFile(), app)
            ia.fileFingerprint = fingerprint
            return if (fingerprint.isValid) {
                FINGERPRINT_OF_DOWNLOADED_FILE_OK
            } else {
                ia.apkCache.deleteCache()
                FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE
            }
        }

        @MainThread
        fun fingerprintOfDownloadedFileOk(ia: InstallActivity): State {
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadGood)
            ia.show(R.id.install_activity__retrigger_installation)
            ia.setText(R.id.fingerprintDownloadGoodHash, ia.fileFingerprint.hexString)
            return TRIGGER_INSTALLATION_PROCESS
        }

        @MainThread
        fun triggerInstallationProcess(ia: InstallActivity): State {
            ia.show(R.id.installingApplication)
            val installationFile = ia.apkCache.getCacheFile()
            require(installationFile.exists()) { "Cached file does not exists" }
            ia.appInstaller.install(ia, installationFile)
            return SUCCESS_PAUSE
        }

        @MainThread
        fun userHasInstalledAppSuccessful(ia: InstallActivity): State {
            ia.hide(R.id.installingApplication)
            ia.hide(R.id.install_activity__retrigger_installation)
            ia.show(R.id.installerSuccess)
            ia.viewModel.downloadId?.let { ia.downloadManager.remove(it) }
            return APP_INSTALLATION_HAS_BEEN_REGISTERED
        }

        @MainThread
        suspend fun appInstallationHasBeenRegistered(ia: InstallActivity): State {
            ia.show(R.id.verifyInstalledFingerprint)
            val fingerprint = ia.fingerprintValidator.checkInstalledApp(ia.app)
            ia.appFingerprint = fingerprint
            ia.hide(R.id.verifyInstalledFingerprint)
            return if (fingerprint.isValid) {
                FINGERPRINT_OF_INSTALLED_APP_OK
            } else {
                FAILURE_FINGERPRINT_OF_INSTALLED_APP_INVALID
            }
        }

        @MainThread
        fun fingerprintOfInstalledAppOk(ia: InstallActivity): State {
            ia.show(R.id.fingerprintInstalledGood)
            ia.setText(R.id.fingerprintInstalledGoodHash, ia.appFingerprint.hexString)
            val available = ia.viewModel.updateCheckResult!!.availableResult
            ia.app.detail.appInstallationCallback(ia, available)
            return SUCCESS_STOP
        }

        //===============================================

        @MainThread
        fun failureUnknownSignatureOfInstalledApp(ia: InstallActivity): State {
            ia.show(R.id.unknownSignatureOfInstalledApp)
            return ERROR_STOP
        }

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
        fun failureDownloadManagerDisabled(ia: InstallActivity): State {
            ia.show(R.id.downloadAppIsDisabled)
            return ERROR_STOP
        }

        @MainThread
        fun failureDownloadUnsuccessful(ia: InstallActivity): State {
            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadFileFailed)
            ia.setText(R.id.downloadFileFailedUrl, ia.viewModel.updateCheckResult!!.downloadUrl)
            ia.show(R.id.installerFailed)
            ia.viewModel.downloadId?.let { ia.downloadManager.remove(it) }
            return ERROR_STOP
        }

        @MainThread
        fun failureInvalidFingerprintOfDownloadedFile(ia: InstallActivity): State {
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadBad)
            ia.setText(R.id.fingerprintDownloadBadHashActual, ia.fileFingerprint.hexString)
            ia.setText(R.id.fingerprintDownloadBadHashExpected, ia.app.detail.signatureHash)
            ia.show(R.id.installerFailed)
            ia.viewModel.downloadId?.let { ia.downloadManager.remove(it) }
            return ERROR_STOP
        }

        @MainThread
        fun failureAppInstallation(ia: InstallActivity): State {
            ia.hide(R.id.installingApplication)
            ia.hide(R.id.install_activity__retrigger_installation)
            ia.show(R.id.installerFailed)
            var error = ia.appInstallationFailedErrorMessage
            if (error != null) {
                if (error.contains("INSTALL_FAILED_INTERNAL_ERROR") &&
                    error.contains("Permission Denied")
                ) {
                    val help =
                        ia.getString(R.string.install_activity__try_disable_miui_optimization)
                    error += "\n\n" + help
                }
                ia.setText(R.id.installerFailedReason, error)
            }
            ia.viewModel.downloadId?.let { ia.downloadManager.remove(it) }
            return ERROR_STOP
        }

        @MainThread
        fun failureFingerprintOfInstalledAppInvalid(ia: InstallActivity): State {
            ia.show(R.id.fingerprintInstalledBad)
            ia.setText(R.id.fingerprintInstalledBadHashActual, ia.appFingerprint.hexString)
            ia.setText(R.id.fingerprintInstalledBadHashExpected, ia.app.detail.signatureHash)
            ia.viewModel.downloadId?.let { ia.downloadManager.remove(it) }
            return ERROR_STOP
        }

        @MainThread
        fun failureShowFetchUrlException(ia: InstallActivity): State {
            ia.hide(R.id.fetchUrl)
            ia.show(R.id.install_activity__exception)
            val text = ia.viewModel.fetchUrlExceptionText ?: "/"
            ia.setText(R.id.install_activity__exception__text, text)
            val exception = ia.viewModel.fetchUrlException
            if (exception != null) {
                ia.findViewById<TextView>(install_activity__exception__show_button)
                    .setOnClickListener {
                        val description =
                            ia.getString(crash_report__explain_text__install_activity_fetching_url)
                        val intent = CrashReportActivity.createIntent(ia, exception, description)
                        ia.startActivity(intent)
                    }
            }
            return ERROR_STOP
        }
    }
}