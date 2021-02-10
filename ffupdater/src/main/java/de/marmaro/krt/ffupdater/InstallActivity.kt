package de.marmaro.krt.ffupdater

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
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
import androidx.lifecycle.lifecycleScope
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.FingerprintValidator.FingerprintResult
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import james.crasher.Crasher
import kotlinx.coroutines.*
import java.io.IOException
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
    private var downloadManager: DownloadManagerAdapter = DownloadManagerAdapter(getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
    private var fingerprintValidator = FingerprintValidator(packageManager)
    private var app: App? = null
    private var downloadId: Long = -1
    private var state = State.SUCCESS_PAUSE
    private var stateJob: Job? = null
    private var updateCheckResult: UpdateCheckResult? = null
    private var fileFingerprint: FingerprintResult? = null
    private var appFingerprint: FingerprintResult? = null
    private var freeSpaceForDownloading: Long = -1
    private var appInstallationFailedErrorMessage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.download_activity)
        Crasher(this)
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(DeviceEnvironment()))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fingerprintValidator = FingerprintValidator(packageManager)

        app = App.valueOf(intent.extras?.getString(EXTRA_APP_NAME) ?: run {
            finish()
            return
        })
        findViewById<View>(R.id.installConfirmationButton).setOnClickListener {
            restartStateMachine(State.USER_HAS_TRIGGERED_INSTALLATION_PROCESS)
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
        if (intent.action == PACKAGE_INSTALLED_ACTION) {
            val status = intent.extras?.getInt(PackageInstaller.EXTRA_STATUS)
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                // This test app isn't privileged, so the user has to confirm the install.
                startActivity(intent.extras!!.get(Intent.EXTRA_INTENT) as Intent)
                return
            }
            if (status == PackageInstaller.STATUS_SUCCESS) {
                restartStateMachine(State.USER_HAS_INSTALLED_APP_SUCCESSFUL)
            } else {
                val errorMessage = intent.extras?.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
                appInstallationFailedErrorMessage = "($status) $errorMessage"
                restartStateMachine(State.FAILURE_APP_INSTALLATION)
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

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val LOG_TAG = "InstallActivity"
        private const val PACKAGE_INSTALLED_ACTION = "de.marmaro.krt.ffupdater.InstallActivity.SESSION_API_PACKAGE_INSTALLED"
    }

    private enum class State(val action: suspend (InstallActivity) -> State) {
        START({ ia ->
            val app = ia.app!!
            if (app.detail.isInstalled(ia)) {
                if (ia.fingerprintValidator.checkInstalledApp(app.detail).isValid) {
                    INSTALLED_APP_SIGNATURE_CHECKED
                }
            }
            FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP
        }),

        INSTALLED_APP_SIGNATURE_CHECKED({
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                EXTERNAL_STORAGE_IS_ACCESSIBLE
            }
            FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE
        }),

        EXTERNAL_STORAGE_IS_ACCESSIBLE({ ia ->
            val downloadManager = "com.android.providers.downloads"
            try {
                if (ia.packageManager.getApplicationInfo(downloadManager, 0).enabled) {
                    DOWNLOAD_MANAGER_IS_ENABLED
                }
            } catch (e: PackageManager.NameNotFoundException) {
                FAILURE_DOWNLOAD_MANAGER_DISABLED
            }
            FAILURE_DOWNLOAD_MANAGER_DISABLED
        }),

        DOWNLOAD_MANAGER_IS_ENABLED({ ia ->
            val path = ia.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
            val freeBytes = StatFs(path).freeBytes
            ia.freeSpaceForDownloading = freeBytes
            if (freeBytes > 100 * 1024 * 1024) {
                PRECONDITIONS_ARE_CHECKED
            }
            FAILURE_LOW_ON_SPACE
        }),

        PRECONDITIONS_ARE_CHECKED({ ia ->
            val app = ia.app!!
            ia.show(R.id.fetchUrl)
            ia.setText(R.id.fetchUrlTextView, ia.getString(R.string.fetch_url_for_download,
                    ia.getString(app.detail.displayDownloadSource)))
            try {
                val result = app.detail.updateCheckAsync(ia, DeviceEnvironment()).await()
                ia.updateCheckResult = result
                ia.hide(R.id.fetchUrl)
                ia.show(R.id.fetchedUrlSuccess)
                ia.setText(R.id.fetchedUrlSuccessTextView,
                        ia.getString(R.string.fetched_url_for_download_successfully,
                                ia.getString(app.detail.displayDownloadSource)))
                AVAILABLE_METADATA_IS_FETCHED
            } catch (e: Exception) {
                throw InstallActivityFetchException("fail to fetch $app", e)
            }
        }),

        AVAILABLE_METADATA_IS_FETCHED({ ia ->
            ia.show(R.id.downloadingFile)
            val downloadUrl = ia.updateCheckResult!!.downloadUrl
            ia.setText(R.id.downloadingFileUrl, downloadUrl.toString())
            val displayTitle = ia.getString(ia.app!!.detail.displayTitle)
            ia.downloadId = ia.downloadManager.enqueue(ia, downloadUrl, displayTitle)
            DOWNLOAD_IS_ENQUEUED
        }),

        DOWNLOAD_IS_ENQUEUED({ ia ->
            val sleepInterval: Long = 500
            val maxWaitingTime: Long = Duration.ofMinutes(5).toMillis()
            for (i: Long in 1..(maxWaitingTime / sleepInterval)) {
                val result = ia.downloadManager.getStatusAndProgress(ia.downloadId)
                val status = when (result.status) {
                    STATUS_RUNNING -> "running"
                    STATUS_SUCCESSFUL -> "success"
                    STATUS_FAILED -> "failed"
                    STATUS_PAUSED -> "paused"
                    STATUS_PENDING -> "pending"
                    else -> "? ($result.status)"
                }
                ia.setText(R.id.downloadingFileText, ia.getString(
                        R.string.download_application_from_with_status, status))
                ia.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress =
                        result.progress

                when (result.status) {
                    STATUS_FAILED -> FAILURE_DOWNLOAD_UNSUCCESSFUL
                    STATUS_SUCCESSFUL -> DOWNLOAD_WAS_SUCCESSFUL
                }
                delay(sleepInterval)
            }
            FAILURE_DOWNLOAD_UNSUCCESSFUL
        }),

        DOWNLOAD_WAS_SUCCESSFUL({ ia ->
            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadedFile)
            ia.setText(R.id.downloadedFileUrl,
                    ia.updateCheckResult?.downloadUrl.toString())
            ia.show(R.id.verifyDownloadFingerprint)

            val file = ia.downloadManager.getFileForDownloadedFile(ia.downloadId)
            val fingerprint = ia.lifecycleScope.async {
                ia.fingerprintValidator.checkApkFile(file, ia.app!!.detail)
            }.await()
            ia.fileFingerprint = fingerprint
            if (fingerprint.isValid) {
                FINGERPRINT_OF_DOWNLOADED_FILE_OK
            } else {
                FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE
            }
        }),

        FINGERPRINT_OF_DOWNLOADED_FILE_OK({ ia ->
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadGood)
            ia.show(R.id.installConfirmation)
            ia.setText(R.id.fingerprintDownloadGoodHash, ia.fileFingerprint?.hexString ?: "")
            SUCCESS_PAUSE
        }),

        USER_HAS_TRIGGERED_INSTALLATION_PROCESS({ ia ->
            ia.show(R.id.installingApplication)
            val installer = ia.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(MODE_FULL_INSTALL)
            // TODO auslagern in eigene Klasse
            try {
                installer.openSession(installer.createSession(params)).use { session ->
                    val lengthInBytes = ia.downloadManager.getTotalDownloadSize(ia.downloadId).toLong()
                    val downloadUri = ia.downloadManager.getUriForDownloadedFile(ia.downloadId)
                    session.openWrite("package", 0, lengthInBytes).use { packageInSession ->
                        ia.contentResolver.openInputStream(downloadUri).use { apk ->
                            val buffer = ByteArray(16384)
                            var n: Int
                            while (apk!!.read(buffer).also { n = it } >= 0) {
                                packageInSession.write(buffer, 0, n)
                            }
                        }
                    }
                    val intent = Intent(ia, InstallActivity::class.java)
                    intent.action = PACKAGE_INSTALLED_ACTION
                    val pendingIntent = PendingIntent.getActivity(ia, 0, intent, 0)
                    session.commit(pendingIntent.intentSender)
                }
            } catch (e: IOException) {
                throw e //TODO own exception
            }
            SUCCESS_PAUSE
        }),

        USER_HAS_INSTALLED_APP_SUCCESSFUL({ ia ->
            ia.hide(R.id.installingApplication)
            ia.hide(R.id.installConfirmation)
            ia.show(R.id.installerSuccess)
            ia.app!!.detail.installationCallback(ia, ia.updateCheckResult!!.version)
            ia.downloadManager.remove(ia.downloadId)
            APP_INSTALLATION_HAS_BEEN_REGISTERED
        }),

        APP_INSTALLATION_HAS_BEEN_REGISTERED({ ia ->
            ia.show(R.id.verifyInstalledFingerprint)
            val fingerprint = ia.lifecycleScope.async {
                ia.fingerprintValidator.checkInstalledApp(ia.app!!.detail)
            }.await()
            ia.appFingerprint = fingerprint
            ia.hide(R.id.verifyInstalledFingerprint)
            if (fingerprint.isValid) {
                FINGERPRINT_OF_INSTALLED_APP_OK
            } else {
                FAILURE_FINGERPRINT_OF_INSTALLED_APP_INVALID
            }
        }),

        FINGERPRINT_OF_INSTALLED_APP_OK({ ia ->
            ia.show(R.id.fingerprintInstalledGood)
            ia.setText(R.id.fingerprintInstalledGoodHash, ia.appFingerprint?.hexString ?: "")
            SUCCESS_STOP
        }),

        //===============================================

        FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP({ ia ->
            ia.show(R.id.unknownSignatureOfInstalledApp)
            ERROR_STOP
        }),

        FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE({ ia ->
            ia.show(R.id.externalStorageNotAccessible)
            ia.setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
            ERROR_STOP
        }),

        FAILURE_DOWNLOAD_MANAGER_DISABLED({ ia ->
            ia.show(R.id.downloadAppIsDisabled)
            ERROR_STOP
        }),

        FAILURE_LOW_ON_SPACE({ ia ->
            ia.show(R.id.tooLowMemory)
            ia.setText(R.id.tooLowMemoryDescription,
                    ia.getString(R.string.too_low_memory_description, ia.freeSpaceForDownloading))
            ERROR_STOP
        }),

        FAILURE_DOWNLOAD_UNSUCCESSFUL({ ia ->
            ia.hide(R.id.downloadingFile)
            ia.show(R.id.downloadFileFailed)
            ia.setText(R.id.downloadFileFailedUrl,
                    ia.updateCheckResult?.downloadUrl.toString())
            ia.show(R.id.installerFailed)
            ERROR_STOP
        }),

        FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE({ ia ->
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadBad)
            ia.setText(R.id.fingerprintDownloadBadHashActual, ia.fileFingerprint?.hexString ?: "")
            ia.setText(R.id.fingerprintDownloadBadHashExpected, ia.app!!.detail.signatureHash)
            ia.show(R.id.installerFailed)
            ERROR_STOP
        }),

        FAILURE_APP_INSTALLATION({ ia ->
            ia.hide(R.id.installingApplication)
            ia.hide(R.id.installConfirmation)
            ia.show(R.id.installerFailed)
            ia.setText(R.id.installerFailedReason, ia.appInstallationFailedErrorMessage)
            ia.downloadManager.remove(ia.downloadId)
            ERROR_STOP
        }),

        FAILURE_FINGERPRINT_OF_INSTALLED_APP_INVALID({ ia ->
            ia.show(R.id.fingerprintInstalledBad)
            ia.setText(R.id.fingerprintInstalledBadHashActual, ia.appFingerprint?.hexString ?: "")
            ia.setText(R.id.fingerprintInstalledBadHashExpected, ia.app!!.detail.signatureHash)
            ERROR_STOP
        }),

        // SUCCESS_PAUSE => state machine will be restarted externally
        SUCCESS_PAUSE({ SUCCESS_PAUSE }),
        SUCCESS_STOP({ SUCCESS_STOP }),
        ERROR_STOP({ ERROR_STOP });
    }

    private class InstallActivityFetchException(message: String, throwable: Throwable) : Exception(message, throwable)
}