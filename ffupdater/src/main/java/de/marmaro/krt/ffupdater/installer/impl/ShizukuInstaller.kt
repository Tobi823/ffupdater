package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.BuildConfig.APPLICATION_ID
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import dev.rikka.tools.refine.Refine
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.File

@Keep
class ShizukuInstaller(app: App) : SessionInstaller(app, false) {
    override val type = Installer.SESSION_INSTALLER
    override val intentName = "de.marmaro.krt.ffupdater.installer.impl.ShizukuNewInstaller"

    @Throws(IllegalArgumentException::class)
    override suspend fun installApkFile(context: Context, file: File) {
        if (DeviceSdkTester.supportsAndroid9P28()) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        failIfShizukuPermissionIsMissing()
        super.installApkFile(context, file)
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

    override suspend fun openSession(
        context: Context,
        file: File,
        block: suspend (PackageInstaller.Session, Int) -> Unit,
    ): Int {
        // Taken from LSPatch (https://github.com/LSPosed/LSPatch) and AuroraStore:
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/ShizukuInstaller.kt)
        val iPackageInstaller = getPackageInstallerInterface()
        val packageInstaller = getPackageInstaller(iPackageInstaller, context)
        val sessionParams = createSessionParams(context, file)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = getSession(iPackageInstaller, sessionId)
        session.use {
            block(it, sessionId)
        }
        return sessionId
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

    private fun getPackageInstallerInterface(): IPackageInstaller {
        val iPackageManager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        return IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(iPackageManager.packageInstaller.asBinder())
        )
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


    override fun abandonSession(context: Context, sessionId: Int) {
        // do nothing because calling abandonSession causes "SecurityException: Caller has no access to session"
    }
}