package de.marmaro.krt.ffupdater.installer.impl

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.*
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.pm.PackageInstaller.*
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.session_installer__status_failure_aborted
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exceptions.UserInteractionIsRequiredException
import de.marmaro.krt.ffupdater.installer.manifacturer.GeneralInstallResultDecoder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


@Keep
class SessionInstaller(app: App, private val foreground: Boolean) : AbstractAppInstaller(app) {
    override val type = Installer.SESSION_INSTALLER
    private val intentNameForAppInstallationCallback =
        "de.marmaro.krt.ffupdater.installer.impl.SessionInstaller.$foreground"
    private val installationStatus = CompletableDeferred<Boolean>()
    private var intentReceiver: BroadcastReceiver? = null

    override suspend fun executeInstallerSpecificLogic(context: Context, file: File) {
        require(file.exists()) { "File does not exists." }
        return withContext(Dispatchers.Main) {
            registerIntentReceiver(context)
            install(context, file)
            try {
                installationStatus.await()
            } finally {
                unregisterIntentReceiver(context)
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @MainThread
    private fun install(context: Context, file: File) {
        val params = createSessionParams(context, file)
        val sessionId = try {
            context.packageManager.packageInstaller.createSession(params)
        } catch (e: IOException) {
            val errorMessage = context.getString(R.string.session_installer__not_enough_storage, e.message)
            fail(getShortErrorMessage(STATUS_FAILURE_STORAGE), e, STATUS_FAILURE_STORAGE, errorMessage)
            return
        }
        context.packageManager.packageInstaller.openSession(sessionId).use {
            copyApkToSession(it, file)
            val intentSender = createSessionChangeReceiver(context, sessionId)
            it.commit(intentSender)
        }
    }

    private fun createSessionParams(context: Context, file: File): SessionParams {
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        val appImpl = app.findImpl()
        val params = SessionParams(MODE_FULL_INSTALL)
        val displayIcon = appImpl.icon // store display icon id in variable to prevent crash
        params.setAppIcon(BitmapFactory.decodeResource(context.resources, displayIcon))
        params.setAppLabel(context.getString(appImpl.title))
        params.setAppPackageName(appImpl.packageName)
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
        if (DeviceSdkTester.supportsAndroid14()) {
            params.setDontKillApp(true)
        }
        return params
    }

    private fun copyApkToSession(session: Session, file: File) {
        val name = "${app.findImpl().packageName}_${System.currentTimeMillis()}"
        file.inputStream().buffered().use { downloadedFileStream ->
            // don't use buffered because I think this causes the 'Unrecognized stream' exception
            session.openWrite(name, 0, file.length()).use { sessionStream ->
                downloadedFileStream.copyTo(sessionStream)
                sessionStream.flush()
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
            STATUS_FAILURE_CONFLICT -> "The installation failed because it conflicts (or is inconsistent with) with " +
                    "another package already installed on the device."

            STATUS_FAILURE_INCOMPATIBLE -> "The installation failed because it is fundamentally incompatible with " +
                    "this device."

            STATUS_FAILURE_INVALID -> "The installation failed because one or more of the APKs was invalid."
            STATUS_FAILURE_STORAGE -> "The installation failed because of storage issues."
            else -> "The installation failed. Status: $status. Maybe " +
                    "${GeneralInstallResultDecoder.getShortErrorMessage(status)} ?"
        }
        return when (val statusMessage = bundle?.getString(EXTRA_STATUS_MESSAGE)) {
            null -> errorMessage
            else -> "$statusMessage. $errorMessage"
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
            else -> "$statusMessage. $errorMessage"
        }
    }

    private fun requestInstallationPermission(context: Context, bundle: Bundle) {
        if (!foreground) {
            fail(UserInteractionIsRequiredException(bundle.getInt(EXTRA_STATUS), context))
            return
        }

        try {
            // ignore UnsafeIntentLaunchViolation because at least OnePlus needs this exact intent
            @Suppress("DEPRECATION")
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
        val filter = IntentFilter(intentNameForAppInstallationCallback)
        if (DeviceSdkTester.supportsAndroid13()) {
            context.registerReceiver(intentReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(intentReceiver, filter)
        }
    }

    private fun unregisterIntentReceiver(context: Context) {
        context.unregisterReceiver(intentReceiver)
        intentReceiver = null
    }
}