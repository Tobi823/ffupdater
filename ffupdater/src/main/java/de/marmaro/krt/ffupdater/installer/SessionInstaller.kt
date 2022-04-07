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
import de.marmaro.krt.ffupdater.InstallActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import java.io.File
import java.io.IOException

//for API >= 24 (Nougat 7.0)
class SessionInstaller(
    private val appInstalledCallback: () -> Any,
    private val appNotInstalledCallback: (errorMessage: String?) -> Any,
) : AppInstaller {
    override fun onNewIntentCallback(intent: Intent, context: Context) {
        if (intent.action != PACKAGE_INSTALLED_ACTION) return
        val bundle = intent.extras ?: let {
            appNotInstalledCallback("intent.extras is null")
            return@onNewIntentCallback
        }

        when (val status = bundle.getInt(EXTRA_STATUS)) {
            STATUS_PENDING_USER_ACTION -> {
                try {
                    //FFUpdater isn't privileged, so the user has to confirm the install.
                    context.startActivity(bundle.get(Intent.EXTRA_INTENT) as Intent)
                } catch (e: ActivityNotFoundException) {
                    val tip = context.getString(
                        R.string.install_activity__try_disable_miui_optimization
                    )
                    appNotInstalledCallback("${e.message}\n\n$tip")
                }
            }
            STATUS_SUCCESS -> {
                appInstalledCallback()
            }
            STATUS_FAILURE -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure))
            }
            STATUS_FAILURE_ABORTED -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure_aborted))
            }
            STATUS_FAILURE_BLOCKED -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure_blocked))
            }
            STATUS_FAILURE_CONFLICT -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure_conflict))
            }
            STATUS_FAILURE_INCOMPATIBLE -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure_incompatible))
            }
            STATUS_FAILURE_INVALID -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure_invalid))
            }
            STATUS_FAILURE_STORAGE -> {
                appNotInstalledCallback(context.getString(R.string.session_installer__status_failure_storage))
            }
            else -> {
                val errorMessage = bundle.getString(EXTRA_STATUS_MESSAGE)
                appNotInstalledCallback("($status) $errorMessage")
            }
        }
    }

    override fun install(activity: Activity, downloadedFile: File) {
        require(downloadedFile.exists()) { "File does not exists." }
        try {
            return installInternal(activity, downloadedFile)
        } catch (e: IOException) {
            throw SessionInstallerException("Fail to install app.", e)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun installInternal(activity: Activity, downloadedFile: File) {
        val installer = activity.packageManager.packageInstaller
        val params = createSessionParams()
        val id = installer.createSession(params)

        installer.registerSessionCallback(object : SessionSuccessCallback(id) {
            override fun onFinishedForThisSession(success: Boolean) {
                installer.unregisterSessionCallback(this)
                if (success) {
                    appInstalledCallback()
                } else {
                    appNotInstalledCallback(null)
                }
            }
        })

        installer.openSession(id).use { session ->
            try {
                copyApkToSession(downloadedFile, session)
            } catch (e: IOException) {
                session.abandon()
                val error = activity.getString(R.string.session_installer__not_enough_storage, e.message)
                appNotInstalledCallback(error)
                return
            }
            val intentSender = createSessionChangeReceiver(activity)
            session.commit(intentSender)
        }
    }

    private fun createSessionParams(): SessionParams {
        val params = SessionParams(MODE_FULL_INSTALL)
        if (DeviceEnvironment.supportsAndroid12()) {
            params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        }
        return params
    }

    private fun copyApkToSession(downloadedFile: File, session: Session) {
        session.openWrite("package", 0, downloadedFile.length()).use { sessionStream ->
            downloadedFile.inputStream().use { downloadedFileStream ->
                downloadedFileStream.copyTo(sessionStream)
            }
        }
    }

    private fun createSessionChangeReceiver(activity: Activity): IntentSender {
        val intent = Intent(activity, InstallActivity::class.java)
        intent.action = PACKAGE_INSTALLED_ACTION
        val flags = if (DeviceEnvironment.supportsAndroid12()) {
            FLAG_UPDATE_CURRENT + FLAG_MUTABLE
        } else {
            FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        return pendingIntent.intentSender
    }

    class SessionInstallerException(message: String, throwable: Throwable) :
        Exception(message, throwable)

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
            "de.marmaro.krt.ffupdater.installer.SessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}