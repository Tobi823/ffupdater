package de.marmaro.krt.ffupdater.download

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.BaseApp
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class PackageManagerUtil(private val packageManager: PackageManager) {

    @MainThread
    @Throws(FileNotFoundException::class, ApkSignatureNotFoundException::class)
    fun getPackageArchiveInfo(path: String): Signature {
        if (!File(path).exists()) {
            throw FileNotFoundException("File '$path' does not exists.")
        }
        var packageInfo: PackageInfo? = null
        if (DeviceSdkTester.supportsAndroid9()) {
            packageInfo = packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)
        }
        if (packageInfo == null) {
            packageInfo = packageManager.getPackageArchiveInfo(path, GET_SIGNATURES)
        }
        checkNotNull(packageInfo) { "PackageInfo for file '$path' is null." }
        return extractSignature(packageInfo)
    }

    @MainThread
    suspend fun getPackageArchiveVersionNameOrNull(path: String): String? {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageArchiveInfo(path, 0)?.versionName
        }
    }

    @Throws(ApkSignatureNotFoundException::class)
    fun getInstalledAppInfo(app: BaseApp): Signature {
        var packageInfo: PackageInfo? = null
        if (DeviceSdkTester.supportsAndroid9()) {
            packageInfo = packageManager.getPackageInfo(app.packageName, GET_SIGNING_CERTIFICATES)
        }
        if (packageInfo == null) {
            // because GET_SIGNATURES is dangerous on Android 4.4 or lower https://stackoverflow.com/a/39348300
            @SuppressLint("PackageManagerGetSignatures")
            packageInfo = packageManager.getPackageInfo(app.packageName, GET_SIGNATURES)
        }
        checkNotNull(packageInfo) { "PackageInfo for package ${app.packageName} is null." }
        return extractSignature(packageInfo)
    }

    fun getInstalledAppVersionName(packageName: String): String? {
        return try {
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

    @Throws(ApkSignatureNotFoundException::class)
    private fun extractSignature(packageInfo: PackageInfo): Signature {
        if (DeviceSdkTester.supportsAndroid9() && packageInfo.signingInfo != null) {
            val signingInfo = packageInfo.signingInfo
            check(!signingInfo.hasMultipleSigners()) { "App ${packageInfo.packageName} has multiple signers" }
            check(signingInfo.signingCertificateHistory.size == 1) {
                "App ${packageInfo.packageName} has ${signingInfo.signingCertificateHistory.size} certificates."
            }
            return signingInfo.signingCertificateHistory[0]
        }
        if (packageInfo.signatures != null) {
            val signatures = packageInfo.signatures
            check(signatures.size == 1) {
                "App ${packageInfo.packageName} has ${signatures.size} signatures."
            }
            return signatures[0]
        }
        throw ApkSignatureNotFoundException("PackageInfo has no signingInfo and no signatures.")
    }
}