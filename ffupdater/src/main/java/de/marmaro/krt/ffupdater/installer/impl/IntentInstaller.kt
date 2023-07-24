package de.marmaro.krt.ffupdater.installer.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import de.marmaro.krt.ffupdater.installer.error.intent.GeneralInstallResultDecoder
import de.marmaro.krt.ffupdater.installer.error.intent.HuaweiInstallResultDecoder
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import kotlinx.coroutines.CompletableDeferred
import java.io.File


@Keep
class IntentInstaller(
    context: Context,
    private val activityResultRegistry: ActivityResultRegistry,
    app: App,
) : AbstractAppInstaller(app) {
    override val type = Installer.NATIVE_INSTALLER
    private lateinit var appInstallationCallback: ActivityResultLauncher<Intent>
    private val installResult = CompletableDeferred<Boolean>()

    private val appResultCallback = lambda@{ activityResult: ActivityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            installResult.complete(true)
            return@lambda
        }
        val exception = createInstallationFailedException(activityResult, context)
        this.installResult.completeExceptionally(exception)
    }

    private fun createInstallationFailedException(
        activityResult: ActivityResult,
        context: Context,
    ): InstallationFailedException {
        val bundle = activityResult.data?.extras
        val resultCode = activityResult.resultCode
        val installResult = bundle?.getInt("android.intent.extra.INSTALL_RESULT")
        val resultCodeString = "ResultCode: $resultCode, INSTALL_RESULT: $installResult"
        val shortErrorMessage = "${getShortErrorMessage(installResult)} $resultCodeString"
        val translatedErrorMessage = "${getTranslatedErrorMessage(context, installResult)} $resultCodeString"
        return InstallationFailedException(shortErrorMessage, resultCode, translatedErrorMessage)
    }

    override fun onCreate(owner: LifecycleOwner) {
        appInstallationCallback = activityResultRegistry.register(
            ACTIVITY_RESULT_NAME,
            owner,
            StartActivityForResult(),
            appResultCallback
        )
    }

    override suspend fun installApkFile(context: Context, file: File) {
        require(this::appInstallationCallback.isInitialized) { "Call lifecycle.addObserver(...) first!" }
        require(file.exists()) { "File does not exists." }
        installApkFileHelper(context.applicationContext, file)
        installResult.await()
    }

    /**
     * See org.fdroid.fdroid.installer.DefaultInstallerActivity.java from
     * https://github.com/f-droid/fdroidclient
     */
    private fun installApkFileHelper(context: Context, file: File) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = if (DeviceSdkTester.supportsAndroid7Nougat24()) {
            FileProvider.getUriForFile(context.applicationContext, FILE_PROVIDER_AUTHORITY, file)
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
        return listOf(
            { HuaweiInstallResultDecoder.getShortErrorMessage(installResult) },
            { GeneralInstallResultDecoder.getShortErrorMessage(installResult) },
            { "Installation failed." })
            .firstNotNullOf { it() }
    }

    private fun getTranslatedErrorMessage(context: Context, installResult: Int?): String {
        return listOf(
            { HuaweiInstallResultDecoder.getTranslatedErrorMessage(context.applicationContext, installResult) },
            { GeneralInstallResultDecoder.getShortErrorMessage(installResult) },
            { "Installation failed." })
            .firstNotNullOf { it() }
    }

    companion object {
        const val FILE_PROVIDER_AUTHORITY = "de.marmaro.krt.ffupdater.fileprovider"
        const val ACTIVITY_RESULT_NAME = "IntentInstaller_app_installation_callback"
    }
}