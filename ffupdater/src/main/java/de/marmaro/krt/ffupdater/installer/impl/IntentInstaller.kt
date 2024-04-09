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
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.error.intent.GeneralInstallResultDecoder
import de.marmaro.krt.ffupdater.installer.error.intent.HuaweiInstallResultDecoder
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.File


@Keep
class IntentInstaller(
    context: Context,
    private val activityResultRegistry: ActivityResultRegistry,
) : AppInstaller {
    private lateinit var appInstallationCallback: ActivityResultLauncher<Intent>
    private var installFailure = Channel<Exception?>()

    override suspend fun startInstallation(context: Context, file: File, appImpl: AppBase): InstallResult {
        return CertificateVerifier(context, appImpl, file).verifyCertificateBeforeAndAfterInstallation {
            installApkFile(context, file)
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun installApkFile(context: Context, file: File) {
        removePreviousResultsFromChannel()
        require(this::appInstallationCallback.isInitialized) { "Call lifecycle.addObserver(...) first!" }
        require(file.exists()) { "File does not exists." }
        installApkFileHelper(context.applicationContext, file)

        val result = installFailure.receive()
        if (result != null) {
            throw result
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun removePreviousResultsFromChannel() {
        while (!installFailure.isEmpty) {
            installFailure.receive()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        appInstallationCallback = activityResultRegistry.register(
            ACTIVITY_RESULT_NAME,
            owner,
            StartActivityForResult(),
            appResultCallback
        )
    }

    private val appResultCallback = lambda@{ activityResult: ActivityResult ->
        val result = if (activityResult.resultCode == Activity.RESULT_OK) {
            null
        } else {
            createInstallationFailedException(activityResult, context)
        }
        try {
            runBlocking {
                installFailure.send(result)
            }
        } catch (e: Exception) {
            throw RuntimeException("Can't use channel to send installation results", e)
        }
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
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, BuildConfig.APPLICATION_ID)
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