package de.marmaro.krt.ffupdater.installer.impl

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.BuildConfig.APPLICATION_ID
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.error.session.GenericSessionResultDecoder
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.File

@Keep
class ShizukuInstaller : AppInstaller {
    private val intentName = "de.marmaro.krt.ffupdater.installer.impl.ShizukuNewInstaller"

    override suspend fun startInstallation(context: Context, file: File, appImpl: AppBase): InstallResult {
        return CertificateVerifier(context, appImpl, file).verifyCertificateBeforeAndAfterInstallation {
            shizukuInstallApkFile(context, file, appImpl)
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun shizukuInstallApkFile(context: Context, file: File, appImpl: AppBase) {
        if (DeviceSdkTester.supportsAndroid9P28()) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        failIfShizukuPermissionIsMissing()
        require(file.exists()) { "File does not exists." }
        val installStatus = CompletableDeferred<Boolean>()
        val intentReceiver = registerIntentReceiver(context, installStatus)
        installApkFileHelper(context, file, appImpl)
        try {
            installStatus.await()
        } finally {
            withContext(Dispatchers.Main) { context.unregisterReceiver(intentReceiver) }
        }
    }

    private fun failIfShizukuPermissionIsMissing() {
        if (!DeviceSdkTester.supportsAndroid6M23()) {
            throw InstallationFailedException("Shizuku is not supported on this device", -433)
        }

        val permission = try {
            Shizuku.checkSelfPermission()
        } catch (e: IllegalStateException) {
            throw InstallationFailedException(
                "Shizuku is not running. Please start the Shizuku service.",
                -432
            )
        }
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(42)
            throw InstallationFailedException("Missing Shizuku permission. Retry again.", -431)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private suspend fun registerIntentReceiver(
        context: Context,
        installStatus: CompletableDeferred<Boolean>,
    ): BroadcastReceiver {
        val intentReceiver = object : BroadcastReceiver() {
            @Throws(IllegalArgumentException::class)
            override fun onReceive(context: Context?, intent: Intent?) {
                requireNotNull(context)
                requireNotNull(intent)
                val bundle = requireNotNull(intent.extras)
                when (val status = bundle.getInt(PackageInstaller.EXTRA_STATUS)) {
                    PackageInstaller.STATUS_SUCCESS -> installStatus.complete(true)
                    else -> installStatus.completeExceptionally(createInstallFailedException(status, bundle, context))
                }
            }
        }
        withContext(Dispatchers.Main) {
            val filter = IntentFilter(intentName)
            if (DeviceSdkTester.supportsAndroid13T33()) {
                context.registerReceiver(intentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(intentReceiver, filter)
            }
        }
        return intentReceiver
    }

    @MainThread
    private suspend fun installApkFileHelper(context: Context, file: File, appImpl: AppBase): Int {
        return withContext(Dispatchers.Default) {
            openSession(context, file, appImpl) { session, sessionId ->
                copyApkToSession(session, file, appImpl)
                val intentSender = createSessionChangeReceiver(context, sessionId)
                session.commit(intentSender)
            }
        }
    }

    private fun allowAppReplacement(params: PackageInstaller.SessionParams) {
        Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags =
            Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags or
                    PackageManagerHidden.INSTALL_REPLACE_EXISTING
    }

    private suspend fun copyApkToSession(session: PackageInstaller.Session, file: File, appImpl: AppBase) {
        val name = "${appImpl.packageName}_${System.currentTimeMillis()}"
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
        intent.`package` = APPLICATION_ID
        val flags = if (DeviceSdkTester.supportsAndroid12S31()) {
            PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
        return pendingIntent.intentSender
    }

    @Throws(IllegalArgumentException::class)
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

    private suspend fun openSession(
        context: Context,
        file: File,
        appImpl: AppBase,
        block: suspend (PackageInstaller.Session, Int) -> Unit,
    ): Int {
        // Taken from LSPatch (https://github.com/LSPosed/LSPatch) and AuroraStore:
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/ShizukuInstaller.kt)
        val iPackageInstaller = getPackageInstallerInterface()
        val packageInstaller = getPackageInstaller(iPackageInstaller, context)
        val sessionParams = createSessionParams(context, file, appImpl)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = getSession(iPackageInstaller, sessionId)
        session.use {
            block(it, sessionId)
        }
        return sessionId
    }

    private fun createSessionParams(context: Context, file: File, appImpl: AppBase): PackageInstaller.SessionParams {
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val displayIcon = appImpl.icon // store display icon id in variable to prevent crash
        params.setAppIcon(BitmapFactory.decodeResource(context.resources, displayIcon))
        params.setAppLabel(context.getString(appImpl.title))
        params.setAppPackageName(appImpl.packageName)
        params.setSize(file.length())
        if (DeviceSdkTester.supportsAndroid7Nougat24()) params.setOriginatingUid(android.os.Process.myUid())
        if (DeviceSdkTester.supportsAndroid8Oreo26()) params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        if (DeviceSdkTester.supportsAndroid12S31()) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        if (DeviceSdkTester.supportsAndroid14U34()) params.setDontKillApp(true)
        allowAppReplacement(params)
        return params
    }

    private fun getPackageInstallerInterface(): IPackageInstaller {
        val iPackageManager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        return IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(iPackageManager.packageInstaller.asBinder())
        )
    }

    private fun getSession(
        iPackageInstaller: IPackageInstaller,
        sessionId: Int,
    ): PackageInstaller.Session {
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(iPackageInstaller.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    private fun getPackageInstaller(
        iPackageInstaller: IPackageInstaller?,
        context: Context,
    ): PackageInstaller {
        val userId = 0
        val hiddenPackageInstaller = if (DeviceSdkTester.supportsAndroid12S31()) {
            PackageInstallerHidden(iPackageInstaller, APPLICATION_ID, null, userId)
        } else if (DeviceSdkTester.supportsAndroid8Oreo26()) {
            PackageInstallerHidden(iPackageInstaller, APPLICATION_ID, userId)
        } else {
            PackageInstallerHidden(context, context.packageManager, iPackageInstaller, APPLICATION_ID, userId)
        }
        return Refine.unsafeCast(hiddenPackageInstaller)
    }

    private fun createInstallFailedException(
        status: Int,
        bundle: Bundle?,
        context: Context,
    ): InstallationFailedException {
        val shortMessage = GenericSessionResultDecoder.getShortErrorMessage(status, bundle)
        val translatedMessage = GenericSessionResultDecoder.getTranslatedErrorMessage(context, status, bundle)
        return InstallationFailedException(shortMessage, status, translatedMessage)
    }

    companion object {
        private const val ACTION_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
        private const val EXTRA_SESSION_ID = "android.content.pm.extra.SESSION_ID"
    }
}
