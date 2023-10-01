package de.marmaro.krt.ffupdater.installer.impl

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageInstaller.EXTRA_STATUS
import android.content.pm.PackageInstaller.STATUS_FAILURE_STORAGE
import android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION
import android.content.pm.PackageInstaller.STATUS_SUCCESS
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.error.session.GenericSessionResultDecoder.getShortErrorMessage
import de.marmaro.krt.ffupdater.installer.error.session.GenericSessionResultDecoder.getTranslatedErrorMessage
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.exceptions.UserInteractionIsRequiredException
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


@Keep
open class SessionInstaller(app: App, private val foreground: Boolean) : AbstractAppInstaller(app) {
    override val type = Installer.SESSION_INSTALLER
    protected open val intentName = "de.marmaro.krt.ffupdater.installer.impl.SessionInstaller.$foreground"

    override suspend fun installApkFile(context: Context, file: File) {
        require(file.exists()) { "File does not exists." }
        val installStatus = CompletableDeferred<Boolean>()
        val intentReceiver = registerIntentReceiver(context, installStatus)
        val sessionId = installApkFileHelper(context, file)
        try {
            installStatus.await()
        } finally {
            withContext(Dispatchers.Main) { context.unregisterReceiver(intentReceiver) }
            abandonSession(context, sessionId)
        }
    }

    private suspend fun registerIntentReceiver(
        context: Context,
        installStatus: CompletableDeferred<Boolean>,
    ): BroadcastReceiver {
        val intentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                requireNotNull(context)
                requireNotNull(intent)
                val bundle = requireNotNull(intent.extras)
                when (val status = bundle.getInt(EXTRA_STATUS)) {
                    STATUS_PENDING_USER_ACTION -> requestInstallationPermission(context, bundle, installStatus)
                    STATUS_SUCCESS -> installStatus.complete(true)
                    else -> installStatus.completeExceptionally(createInstallFailedException(status, bundle, context))
                }
            }
        }

        val filter = IntentFilter(intentName)
        if (DeviceSdkTester.supportsAndroid13T33()) {
            withContext(Dispatchers.Main) { context.registerReceiver(intentReceiver, filter, RECEIVER_NOT_EXPORTED) }
        } else {
            withContext(Dispatchers.Main) { context.registerReceiver(intentReceiver, filter) }
        }
        return intentReceiver
    }

    private fun createInstallFailedException(
        status: Int,
        bundle: Bundle?,
        context: Context,
    ): InstallationFailedException {
        val shortMessage = getShortErrorMessage(status, bundle)
        val translatedMessage = getTranslatedErrorMessage(context, status, bundle)
        return InstallationFailedException(shortMessage, status, translatedMessage)
    }

    @MainThread
    private suspend fun installApkFileHelper(context: Context, file: File): Int {
        return withContext(Dispatchers.Default) {
            openSession(context, file) { session, sessionId ->
                copyApkToSession(session, file)
                val intentSender = createSessionChangeReceiver(context, sessionId)
                session.commit(intentSender)
            }
        }
    }

    protected open suspend fun openSession(context: Context, file: File, block: suspend (Session, Int) -> Unit): Int {
        val packageInstaller = context.packageManager.packageInstaller
        val params = createSessionParams(context, file)
        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            throw createInstallFailedException(STATUS_FAILURE_STORAGE, null, context.applicationContext)
        }
        packageInstaller.openSession(sessionId).use { block(it, sessionId) }
        return sessionId
    }

    protected fun createSessionParams(context: Context, file: File): SessionParams {
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        val appImpl = app.findImpl()
        val params = SessionParams(MODE_FULL_INSTALL)
        val displayIcon = appImpl.icon // store display icon id in variable to prevent crash
        params.setAppIcon(BitmapFactory.decodeResource(context.resources, displayIcon))
        params.setAppLabel(context.getString(appImpl.title))
        params.setAppPackageName(appImpl.packageName)
        params.setSize(file.length())
        if (DeviceSdkTester.supportsAndroid7Nougat24()) params.setOriginatingUid(android.os.Process.myUid())
        if (DeviceSdkTester.supportsAndroid8Oreo26()) params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        if (DeviceSdkTester.supportsAndroid12S31()) params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        if (DeviceSdkTester.supportsAndroid14U34()) params.setDontKillApp(true)
        allowAppReplacement(params)
        return params
    }

    private fun allowAppReplacement(params: SessionParams) {
        Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags =
            Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags or
                    PackageManagerHidden.INSTALL_REPLACE_EXISTING
    }

    private suspend fun copyApkToSession(session: Session, file: File) {
        val name = "${app.findImpl().packageName}_${System.currentTimeMillis()}"
        withContext(Dispatchers.IO) {
            file.inputStream().buffered().use { downloadStream ->
                // don't use buffered because I think this causes the 'Unrecognized stream' exception
                session.openWrite(name, 0, file.length()).use { sessionStream ->
                    downloadStream.copyTo(sessionStream)
                    sessionStream.flush()
                    session.fsync(sessionStream)
                }
            }
        }
    }

    private fun createSessionChangeReceiver(context: Context, sessionId: Int): IntentSender {
        val intent = Intent(intentName)
        intent.`package` = BuildConfig.APPLICATION_ID
        val flags = if (DeviceSdkTester.supportsAndroid12S31()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
        return pendingIntent.intentSender
    }

    private fun requestInstallationPermission(
        context: Context,
        bundle: Bundle,
        installStatus: CompletableDeferred<Boolean>,
    ) {
        if (!foreground) {
            val exception = UserInteractionIsRequiredException(bundle.getInt(EXTRA_STATUS), context)
            installStatus.completeExceptionally(exception)
            return
        }

        try {
            val newIntent = createConfirmInstallationIntent(bundle)
            context.startActivity(newIntent)
        } catch (e: ActivityNotFoundException) {
            val message = "Installation failed because Activity is not available."
            installStatus.completeExceptionally(InstallationFailedException(message, e, -110, e.message ?: "/"))
        }
    }


    private fun createConfirmInstallationIntent(bundle: Bundle): Intent {
        val originalIntent = if (DeviceSdkTester.supportsAndroid13T33()) {
            bundle.getParcelable(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            bundle.getParcelable(Intent.EXTRA_INTENT) as Intent?
        }
        requireNotNull(originalIntent)

        // create new Intent to hide the "UnsafeIntentLaunchViolation"
        return if (originalIntent.action == ACTION_CONFIRM_INSTALL && originalIntent.hasExtra(EXTRA_SESSION_ID)) {
            val newIntent = Intent(ACTION_CONFIRM_INSTALL)
            val sessionId = originalIntent.extras?.getInt(EXTRA_SESSION_ID)
                ?: originalIntent.extras?.getLong(EXTRA_SESSION_ID)
            newIntent.putExtra(EXTRA_SESSION_ID, sessionId)
            newIntent
        } else {
            originalIntent
        }
    }

    protected open fun abandonSession(context: Context, sessionId: Int) {
        val packageInstaller = context.packageManager.packageInstaller
        if (packageInstaller.getSessionInfo(sessionId) == null) return
        try {
            packageInstaller.abandonSession(sessionId)
        } catch (_: SecurityException) {
        } // ignore exception
    }

    companion object {
        private const val ACTION_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
        private const val EXTRA_SESSION_ID = "android.content.pm.extra.SESSION_ID"
    }
}