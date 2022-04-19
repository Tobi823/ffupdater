package de.marmaro.krt.ffupdater.background

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.SessionSuccessCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.Q)
class BackgroundSessionInstaller(
    private val context: Context,
    private val file: File,
    private val app: App,
) {
    data class InstallResult(val success: Boolean, val errorCode: Int?, val errorMessage: String?)

    private val status = CompletableDeferred<InstallResult>()

    fun installAsync(): Deferred<InstallResult> {
        require(file.exists()) { "File does not exists." }
        try {
            install()
            return status
        } catch (e: IOException) {
            status.completeExceptionally(Exception("Fail to install app.", e))
        } catch (e: Exception) {
            status.completeExceptionally(e)
        }
        return status
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun install() {
        val installer = context.packageManager.packageInstaller
        val params = createSessionParams()
        val id = try {
            installer.createSession(params)
        } catch (e: IOException) {
            val error = context.getString(R.string.session_installer__not_enough_storage, e.message)
            status.complete(InstallResult(false, 6, error))
            return
        }

        installer.registerSessionCallback(object : SessionSuccessCallback(id) {
            override fun onFinishedForThisSession(success: Boolean) {
                installer.unregisterSessionCallback(this)
                val sessionInfo = installer.getSessionInfo(id)
                val errorCode = sessionInfo?.stagedSessionErrorCode
                val errorMessage = sessionInfo?.stagedSessionErrorMessage
                status.complete(InstallResult(success, errorCode, errorMessage))
            }
        })

        installer.openSession(id).use { session ->
            copyApkToSession(file, session)
            val intentSender = createFakeChangeReceiver()
            session.commit(intentSender)
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

    private fun copyApkToSession(downloadedFile: File, session: Session) {
        val name = "${app.detail.packageName}_${System.currentTimeMillis()}"
        session.openWrite(name, 0, file.length()).use { sessionStream ->
            file.inputStream().use { downloadedFileStream ->
                downloadedFileStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }
        }
    }

    private fun createFakeChangeReceiver(): IntentSender {
        val intent = Intent(context, BackgroundSessionInstaller::class.java)
        intent.action = PACKAGE_INSTALLED_ACTION
        val flags = if (DeviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        return pendingIntent.intentSender
    }

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
            "de.marmaro.krt.ffupdater.background.BackgroundSessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}