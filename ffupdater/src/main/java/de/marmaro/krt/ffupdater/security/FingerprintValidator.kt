package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageManager
import android.content.pm.Signature
import de.marmaro.krt.ffupdater.app.BaseApp
import de.marmaro.krt.ffupdater.download.ApkSignatureNotFoundException
import de.marmaro.krt.ffupdater.download.PackageManagerUtil
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory

/**
 * Validation of downloaded and installed application.
 */
class FingerprintValidator(private val packageManager: PackageManager) {

    /**
     * Validate the SHA256 fingerprint of the certificate of the downloaded application as APK file.
     * Takes about 1s -> blocking UI thread is not ok -> suspend
     *
     * @param file APK file
     * @param app  app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     */
    @Throws(UnableCheckApkException::class)
    fun checkApkFile(file: File, app: BaseApp): CertificateValidationResult {
        val signature = try {
            PackageManagerUtil(packageManager).getPackageArchiveInfo(file.absolutePath)
        } catch (e: FileNotFoundException) {
            throw UnableCheckApkException("File does not exists.", e)
        } catch (e: ApkSignatureNotFoundException) {
            throw UnableCheckApkException("Can't find signatures of the APK file.", e)
        }
        return try {
            verifyPackageInfo(signature, app)
        } catch (e: CertificateException) {
            throw UnableCheckApkException("Certificate of APK file is invalid.", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnableCheckApkException("Unknown algorithm for checking APK file.", e)
        }
    }

    /**
     * Validate the SHA256 fingerprint of the certificate of the installed application.
     * Takes about 1ms -> blocking UI thread is ok because thread switching is expensive
     *
     * @param app app
     * @return the fingerprint of the app and if it matched with the stored fingerprint
     * @see [Example on how to generate the certificate fingerprint](https://stackoverflow.com/a/22506133)
     *
     * @see [Another example](https://gist.github.com/scottyab/b849701972d57cf9562e)
     */
    @Throws(UnableCheckApkException::class)
    fun checkInstalledApp(app: BaseApp): CertificateValidationResult {
        val signature = try {
            PackageManagerUtil(packageManager).getInstalledAppInfo(app)
        } catch (e: PackageManager.NameNotFoundException) {
            return CertificateValidationResult(false, "")
        } catch (e: ApkSignatureNotFoundException) {
            throw UnableCheckApkException("Can't find signatures of the APK file.", e)
        }
        return try {
            verifyPackageInfo(signature, app)
        } catch (e: CertificateException) {
            throw UnableCheckApkException("Certificate of APK file is invalid", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnableCheckApkException("Unknown algorithm for checking APK file.", e)
        }
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class)
    private fun verifyPackageInfo(signature: Signature, appDetail: BaseApp): CertificateValidationResult {
        val stream = signature.toByteArray().inputStream().buffered()
        val factory = CertificateFactory.getInstance("X509")
        val certificate = factory.generateCertificate(stream)

        val message = MessageDigest.getInstance("SHA-256")
        val fingerprint = message.digest(certificate.encoded)
        val fingerprintString = fingerprint.joinToString("") {
            String.format("%02x", (it.toInt() and 0xFF))
        }

        val isValid = (fingerprintString == appDetail.signatureHash)
        return CertificateValidationResult(
            isValid = isValid,
            hexString = fingerprintString
        )
    }

    class CertificateValidationResult(val isValid: Boolean, val hexString: String)

    class UnableCheckApkException(message: String, throwable: Throwable) :
        Exception(message, throwable)
}