package de.marmaro.krt.ffupdater.security

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import java.io.File
import java.io.FileNotFoundException

@Keep
class PackageManagerUtil(private val packageManager: PackageManager) {

    @Suppress("DEPRECATION")
    @MainThread
    @Throws(FileNotFoundException::class)
    fun getPackageArchiveInfo(path: String): Signature {
        val file = File(path)
        check(file.exists()) { "File '$path' does not exists." }

        if (DeviceSdkTester.supportsAndroid13()) {
            val flags = PackageManager.PackageInfoFlags.of(GET_SIGNING_CERTIFICATES.toLong())
            extractSignature(packageManager.getPackageArchiveInfo(path, flags))?.let { return it }
        }
        if (DeviceSdkTester.supportsAndroid9()) {
            extractSignature(packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES))?.let { return it }
        }
        extractSignature(packageManager.getPackageArchiveInfo(path, GET_SIGNATURES))?.let { return it }
        throw IllegalArgumentException("APK file has no signature.")
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    fun getInstalledAppInfo(app: AppBase): Signature {
        try {
            if (DeviceSdkTester.supportsAndroid13()) {
                val flags = PackageManager.PackageInfoFlags.of(GET_SIGNING_CERTIFICATES.toLong())
                extractSignature(packageManager.getPackageInfo(app.packageName, flags))?.let { return it }
            }
            if (DeviceSdkTester.supportsAndroid9()) {
                extractSignature(packageManager.getPackageInfo(app.packageName, GET_SIGNING_CERTIFICATES))
                    ?.let { return it }
            }
            extractSignature(packageManager.getPackageInfo(app.packageName, GET_SIGNATURES))?.let { return it }
            throw IllegalArgumentException("Can't extract the signature from app ${app.packageName}.")
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException("app.packageName is not whitelisted in AndroidManifest.xml", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSignature(packageInfo: PackageInfo?): Signature? {
        if (packageInfo == null) {
            return null
        }
        if (DeviceSdkTester.supportsAndroid9() && packageInfo.signingInfo != null) {
            return extractSignature(packageInfo.signingInfo)
        }
        if (packageInfo.signatures != null) {
            return extractSignature(packageInfo.signatures)
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractSignature(signingInfo: SigningInfo): Signature {
        check(!signingInfo.hasMultipleSigners()) { "Multiple signers are not allowed." }
        val signatures = signingInfo.signingCertificateHistory
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Found multiple signatures." }
        return checkNotNull(signatures[0])
    }

    @Suppress("DEPRECATION")
    private fun extractSignature(signatures: Array<Signature>): Signature {
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Found multiple signatures." }
        return signatures[0]
    }

    fun getInstalledAppVersionName(packageName: String): String? {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)?.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}