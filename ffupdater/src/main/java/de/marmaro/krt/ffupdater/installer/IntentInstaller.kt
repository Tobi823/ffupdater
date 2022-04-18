package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class IntentInstaller(
        private val appInstalledCallback: () -> Any,
        private val appNotInstalledCallback: (errorMessage: String) -> Any,
) : AppInstaller {

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                appInstalledCallback()
            } else {
                appNotInstalledCallback("resultCode is '$resultCode'")
            }
        }
    }

    override fun install(activity: Activity, file: File) {
        require(file.exists()) { "File does not exists." }
        try {
            return installInternal(activity, file)
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