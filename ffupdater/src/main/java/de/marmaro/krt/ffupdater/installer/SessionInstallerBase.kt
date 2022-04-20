package de.marmaro.krt.ffupdater.installer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.*
import android.content.pm.PackageInstaller.*
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.session_installer__status_failure_incompatible
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller.InstallResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.N)
abstract class SessionInstallerBase(
    private val context: Context,
    private val app: App,
    private val file: File,
) : SecureAppInstaller(context, app, file) {
    private val installationStatus = CompletableDeferred<InstallResult>()
    private var intentReceiver: BroadcastReceiver? = null

    override suspend fun uncheckInstallAsync(): Deferred<InstallResult> {
        requireNotNull(intentReceiver) { "SessionInstaller is not initialized" }
        require(file.exists()) { "File does not exists." }
        try {
            install()
            return installationStatus
        } catch (e: IOException) {
            installationStatus.completeExceptionally(Exception("Fail to install app.", e))
        } catch (e: Exception) {
            installationStatus.completeExceptionally(e)
        }
        return installationStatus
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun install() {
        val installer = context.packageManager.packageInstaller
        val params = createSessionParams()
        val id = try {
            installer.createSession(params)
        } catch (e: IOException) {
            val error = context.getString(R.string.session_installer__not_enough_storage, e.message)
            failure(STATUS_FAILURE_STORAGE, error)
            return
        }
        installer.openSession(id).use {
            copyApkToSession(it)
            val intentSender = createSessionChangeReceiver(id)
            it.commit(intentSender)
        }
    }

    private fun createSessionParams(): SessionParams {
        val params = SessionParams(MODE_FULL_INSTALL)
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        params.setAppIcon(BitmapFactory.decodeResource(context.resources, app.detail.displayIcon))
        params.setAppLabel(context.getString(app.detail.displayTitle))
        params.setAppPackageName(app.detail.packageName)
        params.setSize(file.length())
        if (DeviceSdkTester.supportsAndroidNougat()) {
            params.setOriginatingUid(android.os.Process.myUid())
        }
        if (DeviceSdkTester.supportsAndroidOreo()) {
            params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        }
        if (DeviceSdkTester.supportsAndroid12()) {
            params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        }
        return params
    }

    private fun copyApkToSession(session: Session) {
        val name = "${app.detail.packageName}_${System.currentTimeMillis()}"
        session.openWrite(name, 0, file.length()).use { sessionStream ->
            file.inputStream().use { downloadedFileStream ->
                downloadedFileStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }
        }
    }

    private fun createSessionChangeReceiver(sessionId: Int): IntentSender {
        val intent = Intent(getIntentNameForAppInstallationCallback())
        intent.`package` = context.packageName
        val flags = if (DeviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
        return pendingIntent.intentSender
    }

    private fun handleAppInstallationResult(intent: Intent?) {
        requireNotNull(intent)
        val bundle = requireNotNull(intent.extras)
        when (val status = bundle.getInt(EXTRA_STATUS)) {
            STATUS_PENDING_USER_ACTION -> requestInstallationPermission(bundle)
            STATUS_SUCCESS -> success()
            STATUS_FAILURE -> failure(status, R.string.session_installer__status_failure)
            STATUS_FAILURE_ABORTED -> failure(status, R.string.session_installer__status_failure_aborted)
            STATUS_FAILURE_BLOCKED -> failure(status, R.string.session_installer__status_failure_blocked)
            STATUS_FAILURE_CONFLICT -> failure(status, R.string.session_installer__status_failure_conflict)
            STATUS_FAILURE_INCOMPATIBLE -> failure(status, session_installer__status_failure_incompatible)
            STATUS_FAILURE_INVALID -> failure(status, R.string.session_installer__status_failure_invalid)
            STATUS_FAILURE_STORAGE -> failure(status, R.string.session_installer__status_failure_storage)
            else -> failure(status, "($status) ${bundle.getString(EXTRA_STATUS_MESSAGE)}")
        }
    }

    protected abstract fun requestInstallationPermission(bundle: Bundle)

    protected fun failure(errorCode: Int, errorMessageId: Int) {
        val errorMessage = context.getString(errorMessageId)
        installationStatus.complete(InstallResult(false, errorCode, errorMessage))
    }

    protected fun failure(errorCode: Int, errorMessage: String) {
        installationStatus.complete(InstallResult(false, errorCode, errorMessage))
    }

    protected fun success() {
        installationStatus.complete(InstallResult(true, null, null))
    }

    protected fun registerIntentReceiver() {
        intentReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = handleAppInstallationResult(intent)
        }
        context.registerReceiver(intentReceiver, IntentFilter(getIntentNameForAppInstallationCallback()))
    }

    protected fun unregisterIntentReceiver() {
        context.unregisterReceiver(intentReceiver)
        intentReceiver = null
    }

    protected abstract fun getIntentNameForAppInstallationCallback(): String
}