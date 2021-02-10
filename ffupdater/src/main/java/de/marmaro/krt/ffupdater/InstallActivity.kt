package de.marmaro.krt.ffupdater

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import de.marmaro.krt.ffupdater.app.AppList
import app.UpdateCheckResult
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import james.crasher.Crasher
import kotlinx.coroutines.*
import org.apache.commons.codec.binary.ApacheCodecHex
import java.io.File
import java.io.IOException
import java.io.InputStream
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
    private var app: AppList? = null
    private var downloadId: Long = -1
    private var state = State.START
    private var stateJob: Job? = null
    private var updateCheckResult: UpdateCheckResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.download_activity)
        Crasher(this)
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(DeviceEnvironment()))
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fingerprintValidator = FingerprintValidator(packageManager)

        app = AppList.valueOf(intent.extras?.getString(EXTRA_APP_NAME) ?: run {
            finish()
            return
        })
        findViewById<View>(R.id.installConfirmationButton).setOnClickListener { install() }

        stateJob = lifecycleScope.launch(Dispatchers.Main) {
            executeStateMachine()
        }

        fetchAvailableMetadata()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
        stateJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun executeStateMachine() {
        while (state != State.ERROR_STOP && state != State.SUCCESS_STOP) {
            state = state.action(this)
        }
    }

    private fun fetchAvailableMetadata() {
//        show(R.id.fetchUrl)
//        setText(R.id.fetchUrlTextView, getString(
//                R.string.fetch_url_for_download,
//                app.getDownloadSource(this)))
//        val future: Future<AvailableMetadata> = availableMetadataFetcher.fetchMetadata(app)
//        Thread {
//            Thread.setDefaultUncaughtExceptionHandler(crasher)
//            try {
//                metadata = future[MAX_WAIT_TIME.seconds, TimeUnit.SECONDS]
//                hide(R.id.fetchUrl)
//                show(R.id.fetchedUrlSuccess)
//                setText(R.id.fetchedUrlSuccessTextView, getString(R.string.fetched_url_for_download_successfully,
//                        app.getDownloadSource(this)))
        downloadApplication()
//            } catch (e: Exception) {
//                throw Exception("Failed to fetch the download url from ${app.getDownloadSource(this)}", e) //TODO
//            }
//        }.start()
    }

    private fun downloadApplication() {
//        show(R.id.downloadingFile)
//        setText(R.id.downloadingFileUrl, metadata.getDownloadUrl().toString())
//        downloadId = downloadManager.enqueue(this, metadata.getDownloadUrl(), app.getTitle(this))
//        activeDownloadStatusRefresher()
    }

//    private fun activeDownloadStatusRefresher() {
//        val start = LocalDateTime.now()
//        val maxWaitingTime = Duration.ofMinutes(5)
//        val executor = Executors.newSingleThreadScheduledExecutor()
//        executor.scheduleWithFixedDelay({
//            Thread.setDefaultUncaughtExceptionHandler(crasher)
//            val result: DownloadManagerAdapter.StatusProgress = downloadManager.getStatusAndProgress(downloadId)
//            val status = when (result.status) {
//                STATUS_RUNNING -> "running"
//                STATUS_SUCCESSFUL -> "success"
//                STATUS_FAILED -> "failed"
//                STATUS_PAUSED -> "paused"
//                STATUS_PENDING -> "pending"
//                else -> "? ($result.status)"
//            }
//            setText(R.id.downloadingFileText, getString(R.string.download_application_from_with_status, status))
//            runOnUiThread {
//                (findViewById<View>(R.id.downloadingFileProgressBar) as ProgressBar).progress = result.progress
//            }
//            when (result.status) {
//                DownloadManager.STATUS_FAILED, DownloadManager.STATUS_SUCCESSFUL -> executor.shutdown()
//            }
//            if (CompareHelper(Duration.between(start, LocalDateTime.now())).isGreaterThan(maxWaitingTime)) {
//                executor.shutdown()
//            }
//        }, 0, 500, TimeUnit.MILLISECONDS)
//    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras?.getLong(EXTRA_DOWNLOAD_ID) ?: return != downloadId) {
                // received an older message - skip
                return
            }

            val downloadManagerStatus = downloadManager.getStatusAndProgress(downloadId).status
            // security check; don't trick the state machine in installing the app
            if (downloadManagerStatus == STATUS_SUCCESSFUL) {
                check(state == State.SUCCESS_STOP)
            }
            state = when (downloadManagerStatus) {
                STATUS_FAILED -> State.FAILURE_DOWNLOAD_UNSUCCESSFUL
                STATUS_SUCCESSFUL -> State.DOWNLOAD_WAS_SUCCESSFUL
                else -> State.FAILURE_DOWNLOAD_UNSUCCESSFUL
            }
            stateJob?.cancel()
            stateJob = lifecycleScope.launch(Dispatchers.Main) {
                executeStateMachine()
            }
        }
    }

    /**
     * See example: https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
     */
    private fun install() {
        show(R.id.installingApplication)
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        try {
            installer.openSession(installer.createSession(params)).use { session ->
                val lengthInBytes: Int = downloadManager.getTotalDownloadSize(downloadId)
                val download: Uri = downloadManager.getUriForDownloadedFile(downloadId)
                session.openWrite("package", 0, lengthInBytes.toLong()).use { packageInSession ->
                    getContentResolver().openInputStream(download).use { apk ->
                        Objects.requireNonNull<InputStream>(apk)
                        val buffer = ByteArray(16384)
                        var n: Int
                        while (apk.read(buffer).also { n = it } >= 0) {
                            packageInSession.write(buffer, 0, n)
                        }
                    }
                }
                val context: Context = this@InstallActivity
                val intent = Intent(context, InstallActivity::class.java)
                intent.setAction(PACKAGE_INSTALLED_ACTION)
                val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                val intentSender: IntentSender = pendingIntent.getIntentSender()
                session.commit(intentSender)
            }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "failed to install APK", e)
        }
    }

    /**
     * This method will be called when the app installation is completed.
     *
     * @param intent intent
     */
    protected override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (PACKAGE_INSTALLED_ACTION == intent.getAction()) {
            val extras: Bundle = Objects.requireNonNull<Bundle>(intent.getExtras())
            val status: Int = extras.getInt(PackageInstaller.EXTRA_STATUS)
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                // This test app isn't privileged, so the user has to confirm the install.
                startActivity(extras.get(Intent.EXTRA_INTENT) as Intent)
                return
            }
            hide(R.id.installingApplication)
            hide(R.id.installConfirmation)
            if (status == PackageInstaller.STATUS_SUCCESS) {
                show(R.id.installerSuccess)
                actionVerifyInstalledAppSignature()
                installedMetadataRegister.saveReleaseId(app, metadata.getReleaseId())
            } else {
                show(R.id.installerFailed)
                val message: String = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val text = String.format(Locale.getDefault(), "(%d) %s", status, message)
                setText(R.id.installerFailedReason, text)
            }
            downloadManager.remove(downloadId)
        }
    }

    private fun actionVerifyInstalledAppSignature() {
        show(R.id.verifyInstalledFingerprint)
        Thread {
            Thread.setDefaultUncaughtExceptionHandler(crasher)
            val fingerprintResult: FingerprintResult = fingerprintValidator.checkInstalledApp(app)
            hide(R.id.verifyInstalledFingerprint)
            if (fingerprintResult.isValid) {
                show(R.id.fingerprintInstalledGood)
                setText(R.id.fingerprintInstalledGoodHash, fingerprintResult.hexString)
            } else {
                show(R.id.fingerprintInstalledBad)
                setText(R.id.fingerprintInstalledBadHashActual, fingerprintResult.hexString)
                setText(R.id.fingerprintInstalledBadHashExpected, ApacheCodecHex.encodeHexString(app.getSignatureHash()))
            }
        }.start()
    }

    private fun show(viewId: Int) {
        runOnUiThread { findViewById<View>(viewId).visibility = View.VISIBLE }
    }

    private fun hide(viewId: Int) {
        runOnUiThread { findViewById<View>(viewId).visibility = View.GONE }
    }

    private fun setText(textId: Int, text: String) {
        runOnUiThread { findViewById<TextView>(textId).text = text }
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val LOG_TAG = "InstallActivity"
        private const val PACKAGE_INSTALLED_ACTION = "de.marmaro.krt.ffupdater.InstallActivity.SESSION_API_PACKAGE_INSTALLED"
        private val MAX_WAIT_TIME = Duration.ofSeconds(30)
    }

    private enum class State(val action: suspend (InstallActivity) -> State) {
        START({ activity ->
            val app = activity.app!!
            if (app.impl.isInstalled(activity)) {
                if (activity.fingerprintValidator.checkInstalledApp(app.impl).isValid) {
                    INSTALLED_APP_SIGNATURE_CHECKED
                }
            }
            FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP
        }),

        INSTALLED_APP_SIGNATURE_CHECKED({ activity ->
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                EXTERNAL_STORAGE_IS_ACCESSIBLE
            }
            FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE
        }),

        EXTERNAL_STORAGE_IS_ACCESSIBLE({ activity ->
            val downloadManager = "com.android.providers.downloads"
            try {
                if (activity.packageManager.getApplicationInfo(downloadManager, 0).enabled) {
                    DOWNLOAD_MANAGER_IS_ENABLED
                }
            } catch (e: PackageManager.NameNotFoundException) {
                FAILURE_DOWNLOAD_MANAGER_DISABLED
            }
            FAILURE_DOWNLOAD_MANAGER_DISABLED
        }),

        DOWNLOAD_MANAGER_IS_ENABLED({ activity ->
            val path = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
            if (StatFs(path).freeBytes > 100 * 1024 * 1024) {
                PRECONDITIONS_ARE_CHECKED
            }
            FAILURE_LOW_ON_SPACE
        }),

        PRECONDITIONS_ARE_CHECKED({ activity ->
            val app = activity.app!!
            activity.show(R.id.fetchUrl)
            activity.setText(R.id.fetchUrlTextView,
                    activity.getString(R.string.fetch_url_for_download,
                            activity.getString(app.impl.displayDownloadSource)))
            try {
                val result = app.impl.updateCheckAsync(activity, DeviceEnvironment().abis[0]).await()
                activity.updateCheckResult = result
                activity.hide(R.id.fetchUrl)
                activity.show(R.id.fetchedUrlSuccess)
                activity.setText(R.id.fetchedUrlSuccessTextView,
                        activity.getString(R.string.fetched_url_for_download_successfully,
                                activity.getString(app.impl.displayDownloadSource)))
                AVAILABLE_METADATA_IS_FETCHED
            } catch (e: Exception) {
                throw InstallActivityFetchException("fail to fetch $app", e)
            }
        }),

        AVAILABLE_METADATA_IS_FETCHED({ acti ->
            acti.show(R.id.downloadingFile)
            val downloadUrl = acti.updateCheckResult!!.downloadUrl
            acti.setText(R.id.downloadingFileUrl, downloadUrl.toString())
            acti.downloadId = acti.downloadManager.enqueue(acti,
                    downloadUrl,
                    acti.getString(acti.app!!.impl.displayTitle))
            DOWNLOAD_IS_ENQUEUED
        }),

        DOWNLOAD_IS_ENQUEUED({ activity ->
            val sleepInterval: Long = 500
            val maxWaitingTime: Long = Duration.ofMinutes(5).toMillis()
            for (i: Long in 1..(maxWaitingTime / sleepInterval)) {
                val result = activity.downloadManager.getStatusAndProgress(activity.downloadId)
                val status = when (result.status) {
                    STATUS_RUNNING -> "running"
                    STATUS_SUCCESSFUL -> "success"
                    STATUS_FAILED -> "failed"
                    STATUS_PAUSED -> "paused"
                    STATUS_PENDING -> "pending"
                    else -> "? ($result.status)"
                }
                activity.setText(R.id.downloadingFileText, activity.getString(
                        R.string.download_application_from_with_status, status))
                activity.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress =
                        result.progress

                when (result.status) {
                    STATUS_FAILED -> ERROR_STOP
                    STATUS_SUCCESSFUL -> SUCCESS_STOP // state machine will be restarted externally
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
            val result = ia.lifecycleScope.async {
                ia.fingerprintValidator.checkApkFile(file, ia.app!!.impl)
            }.await()
            if (result.isValid) {
                ia.setText(R.id.fingerprintDownloadGoodHash, result.hexString) // necessary hack
                FINGERPRINT_OF_DOWNLOADED_FILE_OK
            } else {
                ia.setText(R.id.fingerprintDownloadBadHashActual, result.hexString) // necessary hack
                FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE
            }
        }),

        FINGERPRINT_OF_DOWNLOADED_FILE_OK({ ia ->
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadGood)
            ia.show(R.id.installConfirmation)
            SUCCESS_STOP // state machine will be restarted externally
        }),

        //===============================================

        FAILURE_UNKNOWN_SIGNATURE_OF_INSTALLED_APP({ activity ->
            activity.show(R.id.unknownSignatureOfInstalledApp)
            ERROR_STOP
        }),

        FAILURE_EXTERNAL_STORAGE_NOT_ACCESSIBLE({ activity ->
            activity.show(R.id.externalStorageNotAccessible)
            activity.setText(R.id.externalStorageNotAccessible_state, Environment.getExternalStorageState())
            ERROR_STOP
        }),

        FAILURE_DOWNLOAD_MANAGER_DISABLED({ activity ->
            activity.show(R.id.downloadAppIsDisabled)
            ERROR_STOP
        }),

        FAILURE_LOW_ON_SPACE({ activity ->
            activity.show(R.id.tooLowMemory)
            activity.setText(R.id.tooLowMemoryDescription,
                    activity.getString(R.string.too_low_memory_description, 0)) //TODO string abändern, weil ich nicht die Größe ermitteln kann
            ERROR_STOP
        }),

        FAILURE_DOWNLOAD_UNSUCCESSFUL({ activity ->
            activity.hide(R.id.downloadingFile)
            activity.show(R.id.downloadFileFailed)
            activity.setText(R.id.downloadFileFailedUrl,
                    activity.updateCheckResult.downloadUrl.toString())
            activity.show(R.id.installerFailed)
            ERROR_STOP
        }),

        FAILURE_INVALID_FINGERPRINT_OF_DOWNLOADED_FILE({ ia ->
            ia.hide(R.id.verifyDownloadFingerprint)
            ia.show(R.id.fingerprintDownloadBad)
            ia.setText(R.id.fingerprintDownloadBadHashExpected, ia.app!!.impl.signatureHash)
            ia.show(R.id.installerFailed)
            ERROR_STOP
        }),


        AAAAA({ activity ->
            ERROR_STOP
        }),

        SUCCESS_STOP({ SUCCESS_STOP }),
        ERROR_STOP({ ERROR_STOP }),
        ;


    }

    private class InstallActivityFetchException(message: String, throwable: Throwable) : Exception(message, throwable)
}