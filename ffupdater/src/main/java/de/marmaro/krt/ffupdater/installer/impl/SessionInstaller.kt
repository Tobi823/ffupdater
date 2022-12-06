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
import de.marmaro.krt.ffupdater.R.string.session_installer__status_failure_aborted
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.exception.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exception.UserInteractionIsRequiredException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


class SessionInstaller(
    context: Context,
    private val app: App,
    private val file: File,
    private val foreground: Boolean,
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) : AbstractAppInstaller(app, file) {
    private val intentNameForAppInstallationCallback =
        "de.marmaro.krt.ffupdater.installer.impl.SessionInstaller.$foreground"
    private val installationStatus = CompletableDeferred<Boolean>()
    private var intentReceiver: BroadcastReceiver? = null

    override suspend fun executeInstallerSpecificLogic(context: Context) {
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
        val sessionId = try {
            installer.createSession(params)
        } catch (e: IOException) {
            val errorMessage = context.getString(R.string.session_installer__not_enough_storage, e.message)
            fail(getShortErrorMessage(STATUS_FAILURE_STORAGE), e, STATUS_FAILURE_STORAGE, errorMessage)
            return
        }
        installer.openSession(sessionId).use {
            copyApkToSession(it)
            val intentSender = createSessionChangeReceiver(context, sessionId)
            it.commit(intentSender)
        }
    }

    private fun createSessionParams(context: Context): SessionParams {
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        val params = SessionParams(MODE_FULL_INSTALL)
        val displayIcon = app.impl.icon // store display icon id in variable to prevent crash
        params.setAppIcon(BitmapFactory.decodeResource(context.resources, displayIcon))
        params.setAppLabel(context.getString(app.impl.title))
        params.setAppPackageName(app.impl.packageName)
        params.setSize(file.length())
        if (deviceSdkTester.supportsAndroidNougat()) {
            params.setOriginatingUid(android.os.Process.myUid())
        }
        if (deviceSdkTester.supportsAndroidOreo()) {
            params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        }
        if (deviceSdkTester.supportsAndroid12()) {
            params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        }
        return params
    }

    private fun copyApkToSession(session: Session) {
        val name = "${app.impl.packageName}_${System.currentTimeMillis()}"
        session.openWrite(name, 0, file.length()).use { sessionStream ->
            file.inputStream().use { downloadedFileStream -> downloadedFileStream.copyTo(sessionStream) }
        }
    }

    private fun createSessionChangeReceiver(context: Context, sessionId: Int): IntentSender {
        val intent = Intent(intentNameForAppInstallationCallback)
        intent.`package` = "de.marmaro.krt.ffupdater"
        val flags = if (deviceSdkTester.supportsAndroid12()) {
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
            STATUS_SUCCESS -> {
                installationStatus.complete(true)
            }
            else -> {
                fail(
                    getShortErrorMessage(status, bundle),
                    status,
                    getTranslatedErrorMessage(c, status, bundle)
                )
            }
        }
    }

    private fun getShortErrorMessage(status: Int, bundle: Bundle? = null): String {
        val errorMessage = when (status) {
            STATUS_FAILURE -> "The installation failed in a generic way."
            STATUS_FAILURE_ABORTED -> "The installation failed because it was actively aborted."
            STATUS_FAILURE_BLOCKED -> "The installation failed because it was blocked."
            STATUS_FAILURE_CONFLICT -> "The installation failed because it conflicts (or is inconsistent with) with another package already installed on the device."
            STATUS_FAILURE_INCOMPATIBLE -> "The installation failed because it is fundamentally incompatible with this device."
            STATUS_FAILURE_INVALID -> "The installation failed because one or more of the APKs was invalid."
            STATUS_FAILURE_STORAGE -> "The installation failed because of storage issues."
            else -> "The installation failed. Status: $status."
        }
        return when (val statusMessage = bundle?.getString(EXTRA_STATUS_MESSAGE)) {
            null -> errorMessage
            else -> "$errorMessage Debug message: $statusMessage"
        }
    }

    private fun getTranslatedErrorMessage(c: Context, status: Int, bundle: Bundle? = null): String {
        val errorMessage = when (status) {
            STATUS_FAILURE -> c.getString(R.string.session_installer__status_failure)
            STATUS_FAILURE_ABORTED -> c.getString(session_installer__status_failure_aborted)
            STATUS_FAILURE_BLOCKED -> c.getString(R.string.session_installer__status_failure_blocked)
            STATUS_FAILURE_CONFLICT -> c.getString(R.string.session_installer__status_failure_conflict)
            STATUS_FAILURE_INCOMPATIBLE -> c.getString(R.string.session_installer__status_failure_incompatible)
            STATUS_FAILURE_INVALID -> c.getString(R.string.session_installer__status_failure_invalid)
            STATUS_FAILURE_STORAGE -> c.getString(R.string.session_installer__status_failure_storage)
            else -> "The installation failed. Status: $status."
        }
        return when (val statusMessage = bundle?.getString(EXTRA_STATUS_MESSAGE)) {
            null -> errorMessage
            else -> "$errorMessage Debug message: $statusMessage"
        }
    }

    private fun requestInstallationPermission(context: Context, bundle: Bundle) {
        if (!foreground) {
            fail(UserInteractionIsRequiredException(bundle.getInt(EXTRA_STATUS), context))
            return
        }

        try {
            // ignore UnsafeIntentLaunchViolation because at least OnePlus needs this exact intent
            val requestPermission = bundle.get(Intent.EXTRA_INTENT) as Intent
            context.startActivity(requestPermission)
        } catch (e: ActivityNotFoundException) {
            fail("Installation failed because Activity is not available.", e, -110, e.message ?: "/")
        }
    }

    private fun fail(message: String, errorCode: Int, displayErrorMessage: String) {
        installationStatus.completeExceptionally(
            InstallationFailedException(
                message, errorCode, displayErrorMessage,
            )
        )
    }

    private fun fail(message: String, cause: Throwable, errorCode: Int, displayErrorMessage: String) {
        installationStatus.completeExceptionally(
            InstallationFailedException(
                message, cause, errorCode, displayErrorMessage,
            )
        )
    }

    private fun fail(exception: Exception) {
        installationStatus.completeExceptionally(exception)
    }

    private fun registerIntentReceiver(context: Context) {
        intentReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = handleAppInstallationResult(c, i)
        }
        context.registerReceiver(intentReceiver, IntentFilter(intentNameForAppInstallationCallback))
    }

    private fun unregisterIntentReceiver(context: Context) {
        context.unregisterReceiver(intentReceiver)
        intentReceiver = null
    }
}