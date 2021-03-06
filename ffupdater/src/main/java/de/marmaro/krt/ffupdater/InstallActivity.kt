package de.marmaro.krt.ffupdater

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.FingerprintValidator.FingerprintResult
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import james.crasher.Crasher
import kotlinx.coroutines.*
import java.io.File
import java.time.Duration
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
    private lateinit var downloadManager: DownloadManagerAdapter
    private lateinit var fingerprintValidator: FingerprintValidator
    private lateinit var appInstaller: AppInstaller
    private val deviceEnvironment = DeviceEnvironment()

    // necessary for communication with State enums
    private lateinit var app: App
    private var state = State.SUCCESS_PAUSE
    private var stateJob: Job? = null
    private lateinit var fileFingerprint: FingerprintResult
    private lateinit var appFingerprint: FingerprintResult
    private var freeSpaceForDownloading: Long = -1
    private lateinit var appInstallationFailedErrorMessage: String

    // persistent data across orientation changes
    class InstallActivityViewModel : ViewModel() {
        var app: App? = null
        var downloadId: Long? = null
        var downloadFile: File? = null
        var updateCheckResult: UpdateCheckResult? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.download_activity)
        Crasher(this)
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(DeviceEnvironment()))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fingerprintValidator = FingerprintValidator(packageManager)
        downloadManager = DownloadManagerAdapter(getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
        appInstaller = AppInstaller.create(
                deviceEnvironment = deviceEnvironment,
                appInstalledCallback = {
                    restartStateMachine(State.USER_HAS_INSTALLED_APP_SUCCESSFUL)
                },
                appNotInstalledCallback = { errorMessage ->
                    appInstallationFailedErrorMessage = errorMessage
                    restartStateMachine(State.FAILURE_APP_INSTALLATION)
                })
        app = App.valueOf(intent.extras?.getString(EXTRA_APP_NAME) ?: run {
            finish()
            return
        })
        findViewById<View>(R.id.installConfirmationButton).setOnClickListener {
            restartStateMachine(State.USER_HAS_TRIGGERED_INSTALLATION_PROCESS)
        }

        // make sure that the ViewModel is correct for the current app
        viewModel = ViewModelProvider(this).get(InstallActivityViewModel::class.java)
        if (viewModel.app != null) {
            check(viewModel.app == app)
        }
        viewModel.app = app
        // recover from an orientation change - is the download already running/finished?
        if (viewModel.downloadId != null) {
            restartStateMachine(State.DOWNLOAD_IS_ENQUEUED)
            return
        }

        restartStateMachine(State.START)
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
        super.onActivityResult(requestCode, resultCode, data)
        appInstaller.onActivityResult(requestCode, resultCode, data)
    }

    private fun restartStateMachine(jumpDestination: State) {
        // security check to prevent a illegal restart
        if (state != State.SUCCESS_PAUSE) {
            return
        }
        stateJob?.cancel()
        state = jumpDestination
        stateJob = lifecycleScope.launch(Dispatchers.Main) {
            withTimeout(Duration.ofMinutes(5).toMillis()) {
                while (state != State.ERROR_STOP
                        && state != State.SUCCESS_PAUSE
                        && state != State.SUCCESS_STOP) {
                    state = state.action(this@InstallActivity)
                }
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

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val LOG_TAG = "InstallActivity"
    }

    private enum class State(val action: suspend (InstallActivity) -> State) {
        START(f@{ ia ->
            if (!ia.app.detail.isInstalled(ia)) {
                return@f INSTALLED_APP_SIGNATURE_CHECKED
            }
            if (ia.fingerprintValidator.checkInstalledApp(ia.app).isValid) {
                return@f INSTALLED_APP_SIGNATURE_CHECKED
            }
            return@f FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP
        }),

        INSTALLED_APP_SIGNATURE_CHECKED(f@{
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                return@f EXTERNAL_STORAGE_IS_ACCESSIBLE
            }
            return@f FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE
        }),

        EXTERNAL_STORAGE_IS_ACCESSIBLE(f@{ ia ->
            val downloadManager = "com.android.providers.downloads"
            try {
                if (ia.packageManager.getApplicationInfo(downloadManager, 0).enabled) {
                    return@f DOWNLOAD_MANAGER_IS_ENABLED
                }
            } catch (e: PackageManager.NameNotFoundException) {
                return@f FAILURE_DOWNLOAD_MANAGER_DISABLED
            }
            return@f FAILURE_DOWNLOAD_MANAGER_DISABLED
        }),

        DOWNLOAD_MANAGER_IS_ENABLED(f@{ ia ->
            val path = ia.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
            val freeBytes = StatFs(path).freeBytes
            ia.freeSpaceForDownloading = freeBytes
            if (freeBytes > 100 * 1024 * 1024) {
                return@f PRECONDITIONS_ARE_CHECKED
            }
            return@f FAILURE_LOW_ON_SPACE
        }),

        PRECONDITIONS_ARE_CHECKED(f@{ ia ->
            val app = ia.app
            ia.show(R.id.fetchUrl)
            ia.setText(R.id.fetchUrlTextView, ia.getString(R.string.fetch_url_for_download,
                    ia.getString(app.detail.displayDownloadSource)))
            try {
                ia.viewModel.updateCheckResult = app.detail.updateCheck(ia, DeviceEnvironment())
                ia.hide(R.id.fetchUrl)
                ia.show(R.id.fetchedUrlSuccess)
                ia.setText(R.id.fetchedUrlSuccessTextView,
                        ia.getString(R.string.fetched_url_for_download_successfully,
                                ia.getString(app.detail.displayDownloadSource)))
                return@f AVAILABLE_METADATA_IS_FETCHED
            } catch (e: Exception) {
                throw InstallActivityFetchException("fail to fetch $app", e)
            }
        }),

        AVAILABLE_METADATA_IS_FETCHED(f@{ ia ->
            ia.show(R.id.downloadingFile)
            val downloadUrl = ia.viewModel.updateCheckResult!!.downloadUrl
            ia.setText(R.id.downloadingFileUrl, downloadUrl.toString())
            val displayTitle = ia.getString(ia.app.detail.displayTitle)

            val fileReservation = ia.downloadManager.reserveFile(ia)
            ia.viewModel.downloadFile = fileReservation.file
            ia.viewModel.downloadId = ia.downloadManager.enqueue(ia, downloadUrl, displayTitle,
                    fileReservation)
            return@f DOWNLOAD_IS_ENQUEUED
        }),

        DOWNLOAD_IS_ENQUEUED(f@{ ia ->
            val sleepInterval = 250L
            val maxWaitingTime = Duration.ofMinutes(5).toMillis()
            for (i in 1..(maxWaitingTime / sleepInterval)) {
                val r = ia.downloadManager.getStatusAndProgress(ia.viewModel.downloadId!!)
                ia.setText(R.id.downloadingFileText, r.toTranslatedText(ia))
                ia.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = r.progress
                when (r.status) {
                    STATUS_FAILED -> return@f FAILURE_DOWNLOAD_UNSUCCESSFUL
                    STATUS_SUCCESSFUL -> return@f DOWNLOAD_WAS_SUCCESSFUL
                }
                delay(sleepInterval)
            }
            return@f FAILURE_DOWNLOAD_UNSUCCESSFUL
        }),

        DOWNLOAD_WAS_SUCCESSFUL(f@{ ia ->
            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadedFile)
            ia.setText(R.id.downloadedFileUrl,
                    ia.viewModel.updateCheckResult!!.downloadUrl.toString())
            ia.show(R.id.verifyDownloadFingerprint)

            val fingerprint = ia.lifecycleScope.async(Dispatchers.IO) {
                ia.fingerprintValidator.checkApkFile(ia.viewModel.downloadFile!!, ia.app)
            }.await()
            ia.fileFingerprint = fingerprint
            if (fingerprint.isValid) {
                return@f FINGERPRINT_OF_DOWNLOADED_FILE_OK
            } else {
                return@f FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE
            }
        }),

        FINGERPRINT_OF_DOWNLOADED_FILE_OK(f@{ ia ->
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadGood)
            ia.show(R.id.installConfirmation)
            ia.setText(R.id.fingerprintDownloadGoodHash, ia.fileFingerprint.hexString)
            return@f SUCCESS_PAUSE
        }),

        USER_HAS_TRIGGERED_INSTALLATION_PROCESS(f@{ ia ->
            ia.show(R.id.installingApplication)
            ia.appInstaller.install(ia, ia.downloadManager, ia.viewModel.downloadId!!,
                    ia.viewModel.downloadFile!!, ia.deviceEnvironment)
            return@f SUCCESS_PAUSE
        }),

        USER_HAS_INSTALLED_APP_SUCCESSFUL(f@{ ia ->
            ia.hide(R.id.installingApplication)
            ia.hide(R.id.installConfirmation)
            ia.show(R.id.installerSuccess)
            ia.downloadManager.remove(ia.viewModel.downloadId!!)
            return@f APP_INSTALLATION_HAS_BEEN_REGISTERED
        }),

        APP_INSTALLATION_HAS_BEEN_REGISTERED(f@{ ia ->
            ia.show(R.id.verifyInstalledFingerprint)
            val fingerprint = ia.lifecycleScope.async(Dispatchers.IO) {
                ia.fingerprintValidator.checkInstalledApp(ia.app)
            }.await()
            ia.appFingerprint = fingerprint
            ia.hide(R.id.verifyInstalledFingerprint)
            if (fingerprint.isValid) {
                return@f FINGERPRINT_OF_INSTALLED_APP_OK
            } else {
                return@f FAILURE_FINGERPRINT_OF_INSTALLED_APP_INVALID
            }
        }),

        FINGERPRINT_OF_INSTALLED_APP_OK(f@{ ia ->
            ia.show(R.id.fingerprintInstalledGood)
            ia.setText(R.id.fingerprintInstalledGoodHash, ia.appFingerprint.hexString)
            return@f SUCCESS_STOP
        }),

        //===============================================

        FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP(f@{ ia ->
            ia.show(R.id.unknownSignatureOfInstalledApp)
            return@f ERROR_STOP
        }),

        FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE(f@{ ia ->
            ia.show(R.id.externalStorageNotAccessible)
            ia.setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
            return@f ERROR_STOP
        }),

        FAILURE_DOWNLOAD_MANAGER_DISABLED(f@{ ia ->
            ia.show(R.id.downloadAppIsDisabled)
            return@f ERROR_STOP
        }),

        FAILURE_LOW_ON_SPACE(f@{ ia ->
            ia.show(R.id.tooLowMemory)
            ia.setText(R.id.tooLowMemoryDescription,
                    ia.getString(R.string.too_low_memory_description, ia.freeSpaceForDownloading))
            return@f ERROR_STOP
        }),

        FAILURE_DOWNLOAD_UNSUCCESSFUL(f@{ ia ->
            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadFileFailed)
            ia.setText(R.id.downloadFileFailedUrl,
                    ia.viewModel.updateCheckResult!!.downloadUrl.toString())
            ia.show(R.id.installerFailed)
            return@f ERROR_STOP
        }),

        FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE(f@{ ia ->
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadBad)
            ia.setText(R.id.fingerprintDownloadBadHashActual, ia.fileFingerprint.hexString)
            ia.setText(R.id.fingerprintDownloadBadHashExpected, ia.app.detail.signatureHash)
            ia.show(R.id.installerFailed)
            return@f ERROR_STOP
        }),

        FAILURE_APP_INSTALLATION(f@{ ia ->
            ia.hide(R.id.installingApplication)
            ia.hide(R.id.installConfirmation)
            ia.show(R.id.installerFailed)
            var error = ia.appInstallationFailedErrorMessage
            if (error.contains("INSTALL_FAILED_INTERNAL_ERROR") &&
                    error.contains("Permission Denied")) {
                error += "\n\nIf you are using Xiaomi or MIUI, try disabling 'MIUI Optimization'."
            }
            ia.setText(R.id.installerFailedReason, error)
            ia.downloadManager.remove(ia.viewModel.downloadId!!)
            return@f ERROR_STOP
        }),

        FAILURE_FINGERPRINT_OF_INSTALLED_APP_INVALID(f@{ ia ->
            ia.show(R.id.fingerprintInstalledBad)
            ia.setText(R.id.fingerprintInstalledBadHashActual, ia.appFingerprint.hexString)
            ia.setText(R.id.fingerprintInstalledBadHashExpected, ia.app.detail.signatureHash)
            return@f ERROR_STOP
        }),

        // SUCCESS_PAUSE => state machine will be restarted externally
        SUCCESS_PAUSE(f@{ return@f SUCCESS_PAUSE }),
        SUCCESS_STOP(f@{ return@f SUCCESS_STOP }),
        ERROR_STOP(f@{ return@f ERROR_STOP });
    }

    private class InstallActivityFetchException(message: String, throwable: Throwable) :
            Exception(message, throwable)
}