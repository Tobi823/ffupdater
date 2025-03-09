package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import java.io.File

class CertificateVerifier(contextParam: Context, private val appImpl: AppBase, private val file: File) {
    private val context = contextParam.applicationContext

    suspend fun verifyCertificateBeforeAndAfterInstallation(blockForInstallation: suspend () -> Any): InstallResult {
        val fileCertHash = hasApkCorrectCertificate()
        blockForInstallation()
        hasInstalledAppCorrectCertificate(fileCertHash)
        return InstallResult(fileCertHash)
    }

    private suspend fun hasApkCorrectCertificate(): String {
        val fileResult = try {
            FingerprintValidator.checkApkFile(context.packageManager, file, appImpl)
        } catch (e: Exception) {
            val message = "Can't validate the signature of the APK file."
            val translatedMessage = context.getString(R.string.app_installer__failed_to_validate_signature_of_download)
            throw InstallationFailedException(message, e, translatedMessage)
        }
        val fileCertHash = fileResult.hexString
        if (!fileResult.isValid) {
            val expected = appImpl.signatureHash
            val message = "Signature of downloaded APK is invalid. Expected $expected but was $fileCertHash."
            val errorMessage = context.getString(R.string.app_installer__signature_of_download_is_invalid)
            throw InstallationFailedException(message, errorMessage)
        }
        return fileCertHash
    }

    private suspend fun hasInstalledAppCorrectCertificate(fileCertHash: String) {
        val appResult = try {
            FingerprintValidator.checkInstalledApp(context.packageManager, appImpl)
        } catch (e: Exception) {
            val message = "Failed to check installed app."
            val translated = context.getString(R.string.app_installer__failed_to_verify_signature_of_installed_app)
            throw InstallationFailedException(message, e, translated)
        }
        if (!appResult.isValid || fileCertHash != appResult.hexString) {
            val message = "Installed app is NOT verified"
            val translated = context.getString(R.string.app_installer__installed_app_has_invalid_signature)
            throw InstallationFailedException(message, translated)
        }
    }
}