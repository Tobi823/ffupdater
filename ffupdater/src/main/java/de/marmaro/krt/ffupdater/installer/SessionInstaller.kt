package de.marmaro.krt.ffupdater.installer

import android.annotation.SuppressLint
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
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller.InstallResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.N)
class SessionInstaller<T : Any>(
    private val activity: Context,
    private val file: File,
    private val app: App,
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
        val installer = activity.packageManager.packageInstaller
        val params = createSessionParams()
        val id = try {
            installer.createSession(params)
        } catch (e: IOException) {
            val error = activity.getString(R.string.session_installer__not_enough_storage, e.message)
            status.complete(InstallResult(false, 6, error))
            return
        }
        installer.openSession(id).use { session ->
            copyApkToSession(session)
            val intentSender = createSessionChangeReceiver()
            session.commit(intentSender)
        }
    }

    private fun createSessionParams(): SessionParams {
        val params = SessionParams(MODE_FULL_INSTALL)
        // https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/SessionInstaller.kt
        params.setAppIcon(BitmapFactory.decodeResource(activity.resources, app.detail.displayIcon))
        params.setAppLabel(activity.getString(app.detail.displayTitle))
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

    private fun createSessionChangeReceiver(): IntentSender {
        val intent = Intent(activity, intentReceiverClass)
        intent.action = PACKAGE_INSTALLED_ACTION
        val flags = if (DeviceSdkTester.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        return pendingIntent.intentSender
    }

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
            "de.marmaro.krt.ffupdater.installer.SessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}