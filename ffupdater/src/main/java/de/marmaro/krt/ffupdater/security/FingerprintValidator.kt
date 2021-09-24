package de.marmaro.krt.ffupdater.security

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.os.Build
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.app.App
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
    @Suppress("RedundantSuspendModifier")
    suspend fun checkApkFile(file: File, app: App): FingerprintResult {
        return try {
            require(file.exists()) { "file must exists" }
            val path = file.absolutePath
            val info = packageManager.getPackageArchiveInfo(path, GET_SIGNATURES)
            requireNotNull(info) { "getPackageArchiveInfo() must successful parse file" }
            verifyPackageInfo(info, app)
        } catch (e: CertificateException) {
            throw UnableCheckApkException("certificate of APK file is invalid", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnableCheckApkException("unknown algorithm for checking APK file", e)
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
    @SuppressLint("PackageManagerGetSignatures")
    // because GET_SIGNATURES is dangerous on Android 4.4 or lower https://stackoverflow.com/a/39348300
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun checkInstalledApp(app: App): FingerprintResult {
        return try {
            val packageInfo = packageManager.getPackageInfo(app.detail.packageName, GET_SIGNATURES)
            verifyPackageInfo(packageInfo, app)
        } catch (e: CertificateException) {
            throw UnableCheckApkException("certificate of APK file is invalid", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnableCheckApkException("unknown algorithm for checking APK file", e)
        } catch (e: PackageManager.NameNotFoundException) {
            FingerprintResult(false, "")
        }
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class)
    private fun verifyPackageInfo(packageInfo: PackageInfo, appDetail: App): FingerprintResult {
        check(packageInfo.signatures.isNotEmpty())
        val signatureStream = ByteArrayInputStream(packageInfo.signatures[0].toByteArray())
        val certificate =
            CertificateFactory.getInstance("X509").generateCertificate(signatureStream)
        val currentByteArray = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        val current =
            currentByteArray.joinToString("") { String.format("%02x", (it.toInt() and 0xFF)) }
        return FingerprintResult(
            isValid = (current == appDetail.detail.signatureHash),
            hexString = current
        )
    }

    class FingerprintResult(val isValid: Boolean, val hexString: String)

    class UnableCheckApkException(message: String, throwable: Throwable) :
        Exception(message, throwable)
}