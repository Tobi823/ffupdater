package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageManager
import android.content.pm.Signature
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.download.PackageManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
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
    @MainThread
    suspend fun checkApkFile(file: File, app: App): CertificateValidationResult {
        require(file.exists()) { "File '${file.absoluteFile}' must exists." }
        val path = file.absolutePath
        val signature = PackageManagerUtil.getPackageArchiveInfo(packageManager, path)
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

    @MainThread
    suspend fun checkInstalledApp(app: App): CertificateValidationResult {
        return try {
            val signature = PackageManagerUtil.getInstalledAppInfo(packageManager, app)
            verifyPackageInfo(signature, app)
        } catch (e: CertificateException) {
            throw UnableCheckApkException("certificate of APK file is invalid", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnableCheckApkException("unknown algorithm for checking APK file", e)
        } catch (e: PackageManager.NameNotFoundException) {
            CertificateValidationResult(false, "")
        }
    }

    @MainThread
    @Throws(CertificateException::class, NoSuchAlgorithmException::class)
    private suspend fun verifyPackageInfo(
        signature: Signature,
        appDetail: App
    ): CertificateValidationResult {
        return withContext(Dispatchers.IO) {
            val signatureStream = ByteArrayInputStream(signature.toByteArray())
            val certificateFactory = CertificateFactory.getInstance("X509")
            val certificate = certificateFactory.generateCertificate(signatureStream)

            val messageDigestSha256 = MessageDigest.getInstance("SHA-256")
            val certificateFingerprint = messageDigestSha256.digest(certificate.encoded)
            val certificateFingerprintHexString = certificateFingerprint.joinToString("") {
                String.format("%02x", (it.toInt() and 0xFF))
            }

            val isValid = certificateFingerprintHexString == appDetail.detail.signatureHash
            CertificateValidationResult(
                isValid = isValid,
                hexString = certificateFingerprintHexString
            )
        }
    }

    class CertificateValidationResult(val isValid: Boolean, val hexString: String)

    class UnableCheckApkException(message: String, throwable: Throwable) :
        Exception(message, throwable)
}