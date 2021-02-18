package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import de.marmaro.krt.ffupdater.InstallActivity
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import java.io.File
import java.io.IOException

//für API >= 29 (Q)
class SessionInstaller(
        private val appInstalledCallback: () -> Any,
        private val appNotInstalledCallback: (errorMessage: String) -> Any,
) : AppInstaller {
    override fun onNewIntentCallback(intent: Intent, context: Context) {
        if (intent.action == PACKAGE_INSTALLED_ACTION) {
            val status = intent.extras?.getInt(PackageInstaller.EXTRA_STATUS)
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                // This test app isn't privileged, so the user has to confirm the install.
                context.startActivity(intent.extras!!.get(Intent.EXTRA_INTENT) as Intent)
                return
            }
            if (status == PackageInstaller.STATUS_SUCCESS) {
                appInstalledCallback()
            } else {
                val errorMessage = intent.extras?.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
                appNotInstalledCallback("($status) $errorMessage")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    override fun install(
            activity: Activity,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
            downloadedFile: File,
            deviceEnvironment: DeviceEnvironment,
    ) {
        try {
            return installInternal(activity, downloadManagerAdapter, downloadId)
        } catch (e: IOException) {
            throw SessionInstallerException("fail to install app", e)
        }
    }

    private fun installInternal(
            activity: Activity,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
    ) {
        val installer = activity.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(MODE_FULL_INSTALL)
        val downloadSize = downloadManagerAdapter.getTotalDownloadSize(downloadId)
        val downloadUri = downloadManagerAdapter.getUriForDownloadedFile(downloadId)

        installer.openSession(installer.createSession(params)).use { session ->
            session.openWrite("package", 0, downloadSize).use { packageStream ->
                activity.contentResolver.openInputStream(downloadUri).use { apkStream ->
                    apkStream?.copyTo(packageStream)
                }
            }
            val intent = Intent(activity, InstallActivity::class.java)
            intent.action = PACKAGE_INSTALLED_ACTION
            val pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0)
            session.commit(pendingIntent.intentSender)
        }
    }

    class SessionInstallerException(message: String, throwable: Throwable) : Exception(message, throwable)

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
                "de.marmaro.krt.ffupdater.installer.SessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}