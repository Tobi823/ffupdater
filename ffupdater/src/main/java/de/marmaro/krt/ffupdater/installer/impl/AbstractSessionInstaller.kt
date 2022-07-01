package de.marmaro.krt.ffupdater.installer.impl

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
import android.os.Bundle
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.ShortInstallResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


abstract class AbstractSessionInstaller(
    context: Context,
    private val app: MaintainedApp,
    private val file: File,
) : AbstractAppInstaller(app, file) {
    protected abstract val intentNameForAppInstallationCallback: String

    private val installationStatus = CompletableDeferred<ShortInstallResult>()
    private var intentReceiver: BroadcastReceiver? = null

    override suspend fun executeInstallerSpecificLogic(context: Context): ShortInstallResult {
        require(file.exists()) { "File does not exists." }
        return withContext(Dispatchers.Main) {
            registerIntentReceiver(context)
            install(context)
            try {
                installationStatus.await()
            } finally {
                unregisterIntentReceiver(context)
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @MainThread
    private fun install(context: Context) {
        val params = createSessionParams(context)
        val installer = context.packageManager.packageInstaller
        val id = try {
            installer.createSession(params)
        } catch (e: IOException) {
            val errorMessage = context.getString(R.string.session_installer__not_enough_storage, e.message)
            failure(STATUS_FAILURE_STORAGE, errorMessage)
            return
        }
        installer.registerSessionCallback(fallbackAppInstallationResultListener)
        installer.openSession(id).use {
            copyApkToSession(it)
            val intentSender = createSessionChangeReceiver(context, id)
            it.commit(intentSender)
        }
    }

    private fun createSessionParams(context: Context): SessionParams {
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        val params = SessionParams(MODE_FULL_INSTALL)
        val displayIcon = app.detail.displayIcon // store display icon id in variable to prevent crash
        params.setAppIcon(BitmapFactory.decodeResource(context.resources, displayIcon))
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

    private fun createSessionChangeReceiver(context: Context, sessionId: Int): IntentSender {
        val intent = Intent(intentNameForAppInstallationCallback)
        intent.`package` = "de.marmaro.krt.ffupdater"
        val flags = if (DeviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
        return pendingIntent.intentSender
    }

    private fun handleAppInstallationResult(c: Context?, intent: Intent?) {
        Log.e("SessionInstallerBase", "handleAppInstallationResult")
        requireNotNull(c)
        requireNotNull(intent)
        val bundle = requireNotNull(intent.extras)
        when (val status = bundle.getInt(EXTRA_STATUS)) {
            STATUS_PENDING_USER_ACTION -> requestInstallationPermission(c, bundle)
            STATUS_SUCCESS -> success()
            else -> failure(status, getErrorMessage(c, status, bundle))
        }
    }

    private fun getErrorMessage(c: Context, status: Int, bundle: Bundle): String {
        return when (status) {
            STATUS_FAILURE -> c.getString(R.string.session_installer__status_failure)
            STATUS_FAILURE_ABORTED -> c.getString(R.string.session_installer__status_failure_aborted)
            STATUS_FAILURE_BLOCKED -> c.getString(R.string.session_installer__status_failure_blocked)
            STATUS_FAILURE_CONFLICT -> c.getString(R.string.session_installer__status_failure_conflict)
            STATUS_FAILURE_INCOMPATIBLE -> c.getString(R.string.session_installer__status_failure_incompatible)
            STATUS_FAILURE_INVALID -> c.getString(R.string.session_installer__status_failure_invalid)
            STATUS_FAILURE_STORAGE -> c.getString(R.string.session_installer__status_failure_storage)
            else -> "($status) ${bundle.getString(EXTRA_STATUS_MESSAGE)}"
        }
    }

    private val fallbackAppInstallationResultListener = object : SessionCallback() {
        private val abortedErrorMessage =
            context.getString(R.string.session_installer__status_failure_aborted)

        override fun onCreated(sessionId: Int) {}
        override fun onBadgingChanged(sessionId: Int) {}
        override fun onActiveChanged(sessionId: Int, active: Boolean) {}
        override fun onProgressChanged(sessionId: Int, progress: Float) {}
        override fun onFinished(sessionId: Int, success: Boolean) {
            // this should be called after handleAppInstallationResult() and it is only a fallback
            // if PackageInstaller fail to call handleAppInstallationResult()
            // one installationStatus has been completed, its value can not be changed
            if (!success) {
                Log.e("SessionInstallerBase", "failure2()")
                installationStatus.complete(
                    ShortInstallResult(
                        false,
                        STATUS_FAILURE_ABORTED,
                        abortedErrorMessage
                    )
                )
            }
        }

    }

    protected abstract fun requestInstallationPermission(context: Context, bundle: Bundle)

    protected fun failure(errorCode: Int, errorMessage: String) {
        Log.e("SessionInstallerBase", "failure()")
        installationStatus.complete(ShortInstallResult(false, errorCode, errorMessage))
    }

    protected fun success() {
        Log.e("SessionInstallerBase", "success()")
        installationStatus.complete(ShortInstallResult(true, null, null))
    }

    protected fun registerIntentReceiver(context: Context) {
        intentReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = handleAppInstallationResult(c, i)
        }
        context.registerReceiver(intentReceiver, IntentFilter(intentNameForAppInstallationCallback))
    }

    protected fun unregisterIntentReceiver(context: Context) {
        context.unregisterReceiver(intentReceiver)
        intentReceiver = null
    }
}