package de.marmaro.krt.ffupdater.installer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionCallback
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

        when (val status = bundle.getInt(PackageInstaller.EXTRA_STATUS)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                try {
                    //FFUpdater isn't privileged, so the user has to confirm the install.
                    context.startActivity(bundle.get(Intent.EXTRA_INTENT) as Intent)
                } catch (e: ActivityNotFoundException) {
                    val tip = context.getString(
                        R.string.install_activity__try_disable_miui_optimization)
                    appNotInstalledCallback("${e.message}\n\n$tip")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                appInstalledCallback()
            }
            else -> {
                val errorMessage = bundle.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
                appNotInstalledCallback("($status) $errorMessage")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    override fun install(activity: Activity, downloadedFile: File) {
        try {
            return installInternal(activity, downloadedFile)
        } catch (e: IOException) {
            throw SessionInstallerException("fail to install app", e)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun installInternal(activity: Activity, downloadedFile: File) {
        val installer = activity.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(MODE_FULL_INSTALL)
        if (DeviceEnvironment.supportsAndroid12()) {
            params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)

        //execute callbacks when installation is finished
        installer.registerSessionCallback(object : SessionCallback() {
            override fun onCreated(sessionId: Int) {}
            override fun onBadgingChanged(sessionId: Int) {}
            override fun onActiveChanged(sessionId: Int, active: Boolean) {}
            override fun onProgressChanged(sessionId: Int, progress: Float) {}
            override fun onFinished(sessionId: Int, success: Boolean) {
                if (id == sessionId) {
                    installer.unregisterSessionCallback(this)
                    if (success) {
                        appInstalledCallback()
                    } else {
                        appNotInstalledCallback(null)
                    }
                }
            }
        })

        val openSession = installer.openSession(id)
        openSession.use { session ->
            val bytes = downloadedFile.length()
            session.openWrite("package", 0, bytes).use { packageStream ->
                downloadedFile.inputStream().use { downloadedFileStream ->
                    downloadedFileStream.copyTo(packageStream)
                }
            }
            val intent = Intent(activity, InstallActivity::class.java)
            intent.action = PACKAGE_INSTALLED_ACTION
            val pendingIntent = if (DeviceEnvironment.supportsAndroid12()) {
                PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(activity, 0, intent, 0)
            }
            session.commit(pendingIntent.intentSender)
        }
    }

    class SessionInstallerException(message: String, throwable: Throwable) :
        Exception(message, throwable)

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
            "de.marmaro.krt.ffupdater.installer.SessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}