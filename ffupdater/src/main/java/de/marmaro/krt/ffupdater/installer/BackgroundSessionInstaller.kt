package de.marmaro.krt.ffupdater.installer

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
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.Q)
class BackgroundSessionInstaller(
    private val content: Context,
    private val file: File,
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
        val installer = content.packageManager.packageInstaller
        val params = SessionParams(MODE_FULL_INSTALL)
        if (DeviceSdkTester.supportsAndroid12()) {
            params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)

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
            try {
                copyApkToSession(file, session)
            } catch (e: IOException) {
                val error = content.getString(R.string.session_installer__not_enough_storage, e.message)
                status.complete(InstallResult(false, 6, error))
                // abandon() will trigger a 2nd complete()-call - but this 2nd call will be ignored
                session.abandon()
                return
            }
            val intentSender = createFakeChangeReceiver(content)
            session.commit(intentSender)
        }
    }

    private fun copyApkToSession(downloadedFile: File, session: Session) {
        session.openWrite("package", 0, downloadedFile.length()).use { sessionStream ->
            downloadedFile.inputStream().use { downloadedFileStream ->
                downloadedFileStream.copyTo(sessionStream)
            }
        }
    }

    private fun createFakeChangeReceiver(content: Context): IntentSender {
        val intent = Intent(content, BackgroundSessionInstaller::class.java)
        intent.action = PACKAGE_INSTALLED_ACTION
        val flags = if (DeviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(content, 0, intent, flags)
        return pendingIntent.intentSender
    }

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
            "de.marmaro.krt.ffupdater.installer.BackgroundSessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}