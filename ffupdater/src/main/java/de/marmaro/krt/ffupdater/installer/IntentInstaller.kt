package de.marmaro.krt.ffupdater.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import de.marmaro.krt.ffupdater.app.App
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class IntentInstaller(
    private val activityResultRegistry: ActivityResultRegistry,
    app: App,
    private val file: File,
) : ForegroundAppInstaller, SecureAppInstaller(app, file) {
    private val status = CompletableDeferred<AppInstaller.InstallResult>()
    private lateinit var appInstallationCallback: ActivityResultLauncher<Intent>

    override fun onCreate(owner: LifecycleOwner) {
        appInstallationCallback = activityResultRegistry.register(
            "IntentInstaller_app_installation_callback",
            owner,
            StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                status.complete(AppInstaller.InstallResult(true, null, null))
                return@register
            }

            val installResult = it.data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
            val errorMessage = "resultCode: ${it.resultCode}, INSTALL_RESULT: $installResult"
            status.complete(AppInstaller.InstallResult(false, it.resultCode, errorMessage))
        }
    }

    override suspend fun uncheckInstallAsync(context: Context): Deferred<AppInstaller.InstallResult> {
        require(this::appInstallationCallback.isInitialized) { "Call lifecycle.addObserver(...) first!" }
        require(file.exists()) { "File does not exists." }
        try {
            installInternal(file)
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
    private fun installInternal(file: File) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = Uri.fromFile(file)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
        appInstallationCallback.launch(intent)
    }
}