package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.install_activity__downloaded_application_is_not_verified
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.InstallResult
import de.marmaro.krt.ffupdater.installer.ShortInstallResult
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

abstract class AbstractAppInstaller(
    private val app: MaintainedApp,
    private val file: File,
) : AppInstaller {
    override suspend fun installAsync(context: Context): Deferred<InstallResult> {
        return withContext(Dispatchers.IO) {
            async {
                install(context)
            }
        }
    }

    private suspend fun install(context: Context): InstallResult {
        val validator = FingerprintValidator(context.packageManager)
        val fileResult = validator.checkApkFile(file, app.detail)
        val fileCertHash = fileResult.hexString
        if (!fileResult.isValid) {
            val errorMessage = context.getString(install_activity__downloaded_application_is_not_verified)
            return InstallResult(false, fileCertHash, -100, errorMessage)
        }

        val installResult = executeInstallerSpecificLogic(context)
        if (!installResult.success) {
            return InstallResult(false, fileCertHash, installResult.errorCode, installResult.errorMessage)
        }

        val appResult = try {
            validator.checkInstalledApp(app.detail)
        } catch (e: Exception) {
            val errorMessage = "Fail to check APK file. Please retry again. Click to view the error report."
            return InstallResult(false, null, -102, errorMessage, e)
        }
        if (!appResult.isValid || fileCertHash != appResult.hexString) {
            val errorMessage = context.getString(R.string.installed_app_is_not_verified)
            return InstallResult(false, appResult.hexString, -101, errorMessage)
        }
        return InstallResult(true, fileCertHash, installResult.errorCode, installResult.errorMessage)
    }

    protected abstract suspend fun executeInstallerSpecificLogic(context: Context): ShortInstallResult
}