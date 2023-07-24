package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R.string.app_installer__failed_to_validate_signature_of_download
import de.marmaro.krt.ffupdater.R.string.app_installer__failed_to_verify_signature_of_installed_app
import de.marmaro.krt.ffupdater.R.string.app_installer__installed_app_has_invalid_signature
import de.marmaro.krt.ffupdater.R.string.app_installer__signature_of_download_is_invalid
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import java.io.File

@Keep
abstract class AbstractAppInstaller(protected val app: App) : AppInstaller {

    @Throws(InstallationFailedException::class)
    override suspend fun startInstallation(context: Context, file: File): InstallResult {
        val appImpl = app.findImpl()
        val fileCertHash = hasApkCorrectCertificate(context.applicationContext, file, appImpl)
        installApkFile(context.applicationContext, file)
        hasInstalledAppCorrectCertificate(context.applicationContext, appImpl, fileCertHash)
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
            val message = "Signature of downloaded APK is invalid. Expected $expected but was $fileCertHash."
            val errorMessage = context.getString(app_installer__signature_of_download_is_invalid)
            throw InstallationFailedException(message, -100, errorMessage)
        }
        return fileCertHash
    }

    protected abstract suspend fun installApkFile(context: Context, file: File)

    private suspend fun hasInstalledAppCorrectCertificate(context: Context, appImpl: AppBase, fileCertHash: String) {
        val appResult = try {
            FingerprintValidator.checkInstalledApp(context.packageManager, appImpl)
        } catch (e: Exception) {
            val message = "Failed to check installed app."
            val translated = context.getString(app_installer__failed_to_verify_signature_of_installed_app)
            throw InstallationFailedException(message, e, -102, translated)
        }
        if (!appResult.isValid || fileCertHash != appResult.hexString) {
            val message = "Installed app is NOT verified"
            val translated = context.getString(app_installer__installed_app_has_invalid_signature)
            throw InstallationFailedException(message, -101, translated)
        }
    }
}