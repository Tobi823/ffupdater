package de.marmaro.krt.ffupdater.installer.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.ForegroundAppInstaller
import de.marmaro.krt.ffupdater.installer.exception.InstallationFailedException
import kotlinx.coroutines.CompletableDeferred
import java.io.File


class IntentInstaller(
    context: Context,
    private val activityResultRegistry: ActivityResultRegistry,
    app: App,
    private val file: File,
) : ForegroundAppInstaller, AbstractAppInstaller(app, file) {
    private val installationStatus = CompletableDeferred<Boolean>()
    private lateinit var appInstallationCallback: ActivityResultLauncher<Intent>

    private val appResultCallback = lambda@{ activityResult: ActivityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            installationStatus.complete(true)
            return@lambda
        }

        val installResult = activityResult.data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
        val errorMessage = when (installResult) {
            -11 -> context.getString(R.string.intent_installer__likely_storage_failure)
            else -> "resultCode: ${activityResult.resultCode}, INSTALL_RESULT: $installResult"
        }
        installationStatus.completeExceptionally(
            InstallationFailedException(
                "Installation failed. $errorMessage",
                activityResult.resultCode,
                errorMessage
            )
        )
    }

    override fun onCreate(owner: LifecycleOwner) {
        appInstallationCallback = activityResultRegistry.register(
            "IntentInstaller_app_installation_callback",
            owner,
            StartActivityForResult(),
            appResultCallback
        )
    }

    override suspend fun executeInstallerSpecificLogic(context: Context) {
        require(this::appInstallationCallback.isInitialized) { "Call lifecycle.addObserver(...) first!" }
        require(file.exists()) { "File does not exists." }
        installInternal(context, file)
        installationStatus.await()
    }

    /**
     * See org.fdroid.fdroid.installer.DefaultInstallerActivity.java from
     * https://github.com/f-droid/fdroidclient
     */
    private fun installInternal(context: Context, file: File) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = if (DeviceSdkTester.supportsAndroidNougat()) {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        } else {
            Uri.fromFile(file)
        }
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
        appInstallationCallback.launch(intent)
    }

    companion object {
        const val FILE_PROVIDER_AUTHORITY = "de.marmaro.krt.ffupdater.fileprovider"
    }
}