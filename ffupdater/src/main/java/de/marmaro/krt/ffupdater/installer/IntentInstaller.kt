package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.installer.AppInstaller.InstallResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class IntentInstaller(
    private val activity: Activity,
    private val file: File,
) : AppInstaller {
    private val status = CompletableDeferred<InstallResult>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                status.complete(InstallResult(true, null, null))
            } else {
                val installResult = data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
                val errorMessage = "resultCode: $resultCode, INSTALL_RESULT: $installResult"
                status.complete(InstallResult(false, resultCode, errorMessage))
            }
        }
    }

    override suspend fun installAsync(): Deferred<InstallResult> {
        require(file.exists()) { "File does not exists." }
        try {
            installInternal(activity, file)
            return status
        } catch (e: IOException) {
            status.completeExceptionally(Exception("fail to install app", e))
        } catch (e: Exception) {
            status.completeExceptionally(e)
        }
        return status
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

    companion object {
        private const val REQUEST_CODE_INSTALL = 0
    }
}