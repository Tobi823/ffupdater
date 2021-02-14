package de.marmaro.krt.ffupdater.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import de.marmaro.krt.ffupdater.InstallActivity
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
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

    override fun install(
            context: Context,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
    ) {
        try {
            return installInternal(context, downloadManagerAdapter, downloadId)
        } catch (e: IOException) {
            throw SessionInstallerException("fail to install app", e)
        }
    }

    private fun installInternal(
            context: Context,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
    ) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(MODE_FULL_INSTALL)
        val downloadSize = downloadManagerAdapter.getTotalDownloadSize(downloadId)
        val downloadUri = downloadManagerAdapter.getUriForDownloadedFile(downloadId)

        installer.openSession(installer.createSession(params)).use { session ->
            session.openWrite("package", 0, downloadSize).use { packageStream ->
                context.contentResolver.openInputStream(downloadUri).use { apkStream ->
                    apkStream?.copyTo(packageStream)
                }
            }
            val intent = Intent(context, InstallActivity::class.java)
            intent.action = PACKAGE_INSTALLED_ACTION
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            session.commit(pendingIntent.intentSender)
        }
    }

    class SessionInstallerException(message: String, throwable: Throwable) : Exception(message, throwable)

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
                "de.marmaro.krt.ffupdater.installer.SessionInstaller.SESSION_API_PACKAGE_INSTALLED"
    }
}