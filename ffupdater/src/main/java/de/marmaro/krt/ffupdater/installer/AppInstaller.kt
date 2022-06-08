package de.marmaro.krt.ffupdater.installer

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.install_activity__downloaded_application_is_not_verified
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.AppInstaller.ExtendedInstallResult
import de.marmaro.krt.ffupdater.installer.AppInstaller.InstallResult
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper.Installer.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

interface AppInstaller {
    data class InstallResult(val success: Boolean, val errorCode: Int?, val errorMessage: String?)
    data class ExtendedInstallResult(
        val success: Boolean,
        val certificateHash: String? = null,
        val errorCode: Int? = null,
        val errorMessage: String? = null,
        val errorException: Throwable? = null
    )

    suspend fun installAsync(context: Context): Deferred<ExtendedInstallResult>
}

abstract class SecureAppInstaller(
    private val app: App,
    private val file: File,
) : AppInstaller {

    protected abstract suspend fun uncheckInstallAsync(context: Context): Deferred<InstallResult>

    override suspend fun installAsync(context: Context): Deferred<ExtendedInstallResult> {
        return withContext(Dispatchers.IO) {
            async {
                secureInstallAsync(context)
            }
        }
    }

    private suspend fun secureInstallAsync(context: Context): ExtendedInstallResult {
        val validator = FingerprintValidator(context.packageManager)
        val fileResult = validator.checkApkFile(file, app.detail)
        val fileCertHash = fileResult.hexString
        if (!fileResult.isValid) {
            val errorMessage = context.getString(install_activity__downloaded_application_is_not_verified)
            return ExtendedInstallResult(false, fileCertHash, -100, errorMessage)
        }

        val installResult = uncheckInstallAsync(context).await()
        val installCode = installResult.errorCode
        val installMessage = installResult.errorMessage
        if (!installResult.success) {
            return ExtendedInstallResult(false, fileCertHash, installCode, installMessage)
        }

        val appResult = try {
            validator.checkInstalledApp(app.detail)
        } catch (e: FingerprintValidator.UnableCheckApkException) {
            val errorMessage = "Fail to check APK file. Please retry again. Click to view the error report."
            return ExtendedInstallResult(false, null, -102, errorMessage, e)
        }
        if (!appResult.isValid || fileCertHash != appResult.hexString) {
            val errorMessage = context.getString(R.string.installed_app_is_not_verified)
            return ExtendedInstallResult(false, appResult.hexString, -101, errorMessage)
        }
        return ExtendedInstallResult(true, fileCertHash, installCode, installMessage)
    }
}

interface BackgroundAppInstaller : AppInstaller {
    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        fun create(context: Context, app: App, file: File): BackgroundAppInstaller {
            return when (InstallerSettingsHelper(context).getInstaller()) {
                SESSION_INSTALLER -> BackgroundSessionInstaller(context, app, file)
                NATIVE_INSTALLER ->
                    throw Exception("The current installer can not update apps in the background")
                ROOT_INSTALLER -> RootInstaller(app, file)
            }
        }
    }
}

interface ForegroundAppInstaller : AppInstaller, DefaultLifecycleObserver {
    companion object {
        fun create(activity: ComponentActivity, app: App, file: File): ForegroundAppInstaller {
            return when (InstallerSettingsHelper(activity).getInstaller()) {
                SESSION_INSTALLER -> ForegroundSessionInstaller(activity, app, file)
                NATIVE_INSTALLER -> IntentInstaller(activity, activity.activityResultRegistry, app, file)
                ROOT_INSTALLER -> RootInstaller(app, file)
            }
        }
    }
}