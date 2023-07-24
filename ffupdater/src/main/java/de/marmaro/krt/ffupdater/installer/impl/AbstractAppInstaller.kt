package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.app_installer__failed_to_validate_signature_of_download
import de.marmaro.krt.ffupdater.R.string.app_installer__signature_of_download_is_invalid
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import java.io.File

@Keep
abstract class AbstractAppInstaller(
    protected val app: App,
) : AppInstaller {

    override suspend fun startInstallation(context: Context, file: File): InstallResult {
        val appImpl = app.findImpl()
        val fileCertHash = hasApkCorrectCertificate(context, file, appImpl)
        // in failure, this will throw InstallationFailedException
        executeInstallerSpecificLogic(context, file)
        hasInstalledAppCorrectCertificate(context, appImpl, fileCertHash)
        return InstallResult(fileCertHash)
    }

    private suspend fun hasApkCorrectCertificate(context: Context, file: File, appImpl: AppBase): String {
        val fileResult = try {
            FingerprintValidator.checkApkFile(context.packageManager, file, appImpl)
        } catch (e: Exception) {
            val message = "Can't validate the signature of the APK file."
            val translatedMessage = context.getString(app_installer__failed_to_validate_signature_of_download)
            throw InstallationFailedException(message, e, -103, translatedMessage)
        }
        val fileCertHash = fileResult.hexString
        if (!fileResult.isValid) {
            val expected = appImpl.signatureHash
            val message = "Downloaded application is NOT verified. Expected $expected but was $fileCertHash."
            val errorMessage = context.getString(app_installer__signature_of_download_is_invalid)
            throw InstallationFailedException(message, -100, errorMessage)
        }
        return fileCertHash
    }

    private suspend fun hasInstalledAppCorrectCertificate(context: Context, appImpl: AppBase, fileCertHash: String) {
        val appResult = try {
            FingerprintValidator.checkInstalledApp(context.packageManager, appImpl)
        } catch (e: Exception) {
            val errorMessage = "Failed to check installed app."
            throw InstallationFailedException("Failed to check installed app.", e, -102, errorMessage)
        }
        if (!appResult.isValid || fileCertHash != appResult.hexString) {
            val errorMessage = context.getString(R.string.installed_app_is_not_verified)
            throw InstallationFailedException("Installed app is NOT verified", -101, errorMessage)
        }
    }

    protected abstract suspend fun executeInstallerSpecificLogic(context: Context, file: File)
}