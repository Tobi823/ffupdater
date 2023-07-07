package de.marmaro.krt.ffupdater.installer.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.installer.manifacturer.GeneralInstallResultDecoder
import de.marmaro.krt.ffupdater.installer.manifacturer.HuaweiInstallResultDecoder
import kotlinx.coroutines.CompletableDeferred
import java.io.File


@Keep
class IntentInstaller(
    context: Context,
    private val activityResultRegistry: ActivityResultRegistry,
    app: App,
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) : AbstractAppInstaller(app) {
    override val type = Installer.NATIVE_INSTALLER
    private lateinit var appInstallationCallback: ActivityResultLauncher<Intent>
    private val installationStatusFromCallback = CompletableDeferred<Boolean>()

    private val appResultCallback = lambda@{ activityResult: ActivityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            installationStatusFromCallback.complete(true)
            return@lambda
        }

        val bundle = activityResult.data?.extras
        val resultCode = activityResult.resultCode
        val installResult = bundle?.getInt("android.intent.extra.INSTALL_RESULT")
        val shortErrorMessage = getShortErrorMessage(installResult)
        val translatedErrorMessage = getTranslatedErrorMessage(context, installResult)
        installationStatusFromCallback.completeExceptionally(
            InstallationFailedException(
                "$shortErrorMessage ResultCode: $resultCode, INSTALL_RESULT: $installResult",
                activityResult.resultCode,
                "$translatedErrorMessage ResultCode: $resultCode, INSTALL_RESULT: $installResult",
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

    override suspend fun executeInstallerSpecificLogic(context: Context, file: File) {
        require(this::appInstallationCallback.isInitialized) { "Call lifecycle.addObserver(...) first!" }
        require(file.exists()) { "File does not exists." }
        installInternal(context, file)
        installationStatusFromCallback.await()
    }

    /**
     * See org.fdroid.fdroid.installer.DefaultInstallerActivity.java from
     * https://github.com/f-droid/fdroidclient
     */
    private fun installInternal(context: Context, file: File) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = if (deviceSdkTester.supportsAndroidNougat()) {
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

    private fun getShortErrorMessage(installResult: Int?): String {
        // https://dev.to/devwithzachary/what-do-mobile-app-installation-result-codes-on-huawei-devices-mean-and-how-to-resolve-them-2a3g
        var message: String? = null
        if (Build.MANUFACTURER == "HUAWEI") {
            message = HuaweiInstallResultDecoder.getShortErrorMessage(installResult)
        }
        if (message == null) {
            message = GeneralInstallResultDecoder.getShortErrorMessage(installResult)
        }
        return message ?: "Installation failed."
    }

    private fun getTranslatedErrorMessage(context: Context, installResult: Int?): String {
        // https://dev.to/devwithzachary/what-do-mobile-app-installation-result-codes-on-huawei-devices-mean-and-how-to-resolve-them-2a3g
        var message: String? = null
        if (Build.MANUFACTURER == "HUAWEI") {
            message = HuaweiInstallResultDecoder.getTranslatedErrorMessage(context, installResult)
        }
        if (message == null) {
            message = GeneralInstallResultDecoder.getShortErrorMessage(installResult)
        }
        return message ?: "Installation failed."
    }

    companion object {
        const val FILE_PROVIDER_AUTHORITY = "de.marmaro.krt.ffupdater.fileprovider"
    }
}