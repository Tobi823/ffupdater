package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageManager
import android.content.pm.Signature
import de.marmaro.krt.ffupdater.app.maintained.AppBase
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
    fun checkApkFile(file: File, app: AppBase): FingerprintValidatorResult {
        val signature = PackageManagerUtil(packageManager).getPackageArchiveInfo(file.absolutePath)
        return verifyPackageInfo(signature, app)
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
    fun checkInstalledApp(app: AppBase): FingerprintValidatorResult {
        val signature = PackageManagerUtil(packageManager).getInstalledAppInfo(app)
        return verifyPackageInfo(signature, app)
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class)
    private fun verifyPackageInfo(signature: Signature, appDetail: AppBase): FingerprintValidatorResult {
        val stream = signature.toByteArray().inputStream().buffered()
        val factory = CertificateFactory.getInstance("X509")
        val certificate = factory.generateCertificate(stream)

        val message = MessageDigest.getInstance("SHA-256")
        val fingerprint = message.digest(certificate.encoded)
        val fingerprintString = fingerprint.joinToString("") {
            String.format("%02x", (it.toInt() and 0xFF))
        }

        val isValid = (fingerprintString == appDetail.signatureHash)
        return FingerprintValidatorResult(
            isValid = isValid,
            hexString = fingerprintString
        )
    }
}