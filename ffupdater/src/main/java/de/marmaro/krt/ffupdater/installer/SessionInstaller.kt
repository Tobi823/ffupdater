package de.marmaro.krt.ffupdater.installer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller.*
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller.InstallResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.N)
class SessionInstaller<T : Any>(
    private val activity: Activity,
    private val file: File,
    private val intentReceiverClass: Class<T>
) : AppInstaller {
    private val status = CompletableDeferred<InstallResult>()

    override fun onNewIntentCallback(intent: Intent, context: Context) {
        if (intent.action != PACKAGE_INSTALLED_ACTION) {
            return
        }
        val bundle = intent.extras ?: return
        val extraStatus = bundle.getInt(EXTRA_STATUS)
        val reportFailure = { message: String ->
            status.complete(InstallResult(false, extraStatus, message))
        }
        when (extraStatus) {
            STATUS_PENDING_USER_ACTION -> {
                try {
                    // request installation permission
                    // but this should never happens because the permission is already requested in MainActivity
                    context.startActivity(bundle.get(Intent.EXTRA_INTENT) as Intent)
                } catch (e: ActivityNotFoundException) {
                    val tip = context.getString(R.string.install_activity__try_disable_miui_optimization)
                    reportFailure("${e.message}\n\n$tip")
                }
            }
            STATUS_SUCCESS -> {
                status.complete(InstallResult(true, null, null))
            }
            STATUS_FAILURE -> {
                reportFailure(context.getString(R.string.session_installer__status_failure))
            }
            STATUS_FAILURE_ABORTED -> {
                reportFailure(context.getString(R.string.session_installer__status_failure_aborted))
            }
            STATUS_FAILURE_BLOCKED -> {
                reportFailure(context.getString(R.string.session_installer__status_failure_blocked))
            }
            STATUS_FAILURE_CONFLICT -> {
                reportFailure(context.getString(R.string.session_installer__status_failure_conflict))
            }
            STATUS_FAILURE_INCOMPATIBLE -> {
                reportFailure(context.getString(R.string.session_installer__status_failure_incompatible))
            }
            STATUS_FAILURE_INVALID -> {
                reportFailure(context.getString(R.string.session_installer__status_failure_invalid))
            }
            STATUS_FAILURE_STORAGE -> {
                reportFailure(context.getString(R.string.session_installer__status_failure_storage))
            }
            else -> {
                reportFailure("($extraStatus) ${bundle.getString(EXTRA_STATUS_MESSAGE)}")
            }
        }
    }

    override suspend fun installAsync(): Deferred<InstallResult> {
        require(file.exists()) { "File does not exists." }
        try {
            installInternal(activity, file)
            return status
        } catch (e: IOException) {
            status.completeExceptionally(Exception("Fail to install app.", e))
        } catch (e: Exception) {
            status.completeExceptionally(e)
        }
        return status
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun installInternal(activity: Activity, downloadedFile: File) {
        val installer = activity.packageManager.packageInstaller
        val params = SessionParams(MODE_FULL_INSTALL)
        if (DeviceSdkTester.supportsAndroid12()) {
            params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        }

        // TODO can I add checksums to SessionManager for more security?
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            try {
                copyApkToSession(downloadedFile, session)
            } catch (e: IOException) {
                val error = activity.getString(R.string.session_installer__not_enough_storage, e.message)
                status.complete(InstallResult(false, 6, error))
                // abandon() will trigger a 2nd complete()-call - but this 2nd call will be ignored
                session.abandon()
                return
            }
            val intentSender = createSessionChangeReceiver(activity)
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

    private fun createSessionChangeReceiver(content: Context): IntentSender {
        val intent = Intent(content, intentReceiverClass)
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
            "de.marmaro.krt.ffupdater.installer.SessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}