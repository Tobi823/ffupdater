package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter
import java.io.File
import java.io.IOException


//für API < 29 (Q)
class IntentInstaller(
        private val appInstalledCallback: () -> Any,
        private val appNotInstalledCallback: (errorMessage: String) -> Any,
) : AppInstaller {

    override fun onNewIntentCallback(intent: Intent, context: Context) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                appInstalledCallback()
            } else {
                appNotInstalledCallback("resultCode is '$resultCode'")
            }
        }
    }

    override fun install(
            activity: Activity,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
            downloadedFile: File,
            deviceEnvironment: DeviceEnvironment,
    ) {
        try {
            return installInternal(activity, downloadManagerAdapter, downloadId, downloadedFile,
                    deviceEnvironment)
        } catch (e: IOException) {
            throw IntentInstallerException("fail to install app", e)
        }
    }

    private fun installInternal(
            activity: Activity,
            downloadManagerAdapter: DownloadManagerAdapter,
            downloadId: Long,
            downloadedFile: File,
            deviceEnvironment: DeviceEnvironment,
    ) {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        if (deviceEnvironment.supportsAndroidNougat()) {
            intent.data = Uri.fromFile(downloadedFile)
        } else {
            intent.data = downloadManagerAdapter.getUriForDownloadedFile(downloadId)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId)
        }
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        activity.startActivityForResult(intent, REQUEST_CODE_INSTALL)
    }

    class IntentInstallerException(message: String, throwable: Throwable) : Exception(message, throwable)


    companion object {
        private val REQUEST_CODE_INSTALL = 0
    }
}