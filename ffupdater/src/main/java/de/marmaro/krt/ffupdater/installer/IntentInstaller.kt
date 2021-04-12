package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.IOException


//for API < 24 (Nougat 7.0)
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

    override fun install(activity: Activity, downloadedFile: File) {
        try {
            return installInternal(activity, downloadedFile)
        } catch (e: IOException) {
            throw IntentInstallerException("fail to install app", e)
        }
    }

    /**
     * See org.fdroid.fdroid.installer.DefaultInstallerActivity.java from
     * https://github.com/f-droid/fdroidclient
     */
    private fun installInternal(activity: Activity, downloadedFile: File) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = Uri.fromFile(downloadedFile)
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        activity.startActivityForResult(intent, REQUEST_CODE_INSTALL)
    }

    class IntentInstallerException(message: String, throwable: Throwable) : Exception(message, throwable)

    companion object {
        private const val REQUEST_CODE_INSTALL = 0
    }
}