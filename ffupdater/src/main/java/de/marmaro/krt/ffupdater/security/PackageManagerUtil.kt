package de.marmaro.krt.ffupdater.security

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.app.maintained.AppBase
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class PackageManagerUtil(private val packageManager: PackageManager) {

    @Suppress("DEPRECATION")
    @MainThread
    @Throws(FileNotFoundException::class)
    fun getPackageArchiveInfo(path: String): Signature {
        if (!File(path).exists()) {
            throw FileNotFoundException("File '$path' does not exists.")
        }

        if (DeviceSdkTester.supportsAndroid9()) {
            packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)
                ?.let { extractSignatureFromSigningInfo(it) }
                ?.let { return it }
        }

        packageManager.getPackageArchiveInfo(path, GET_SIGNATURES)
            ?.let { extractSignatureFromSignatures(it) }
            ?.let { return it }

        throw IllegalArgumentException("Can't extract the signature from the APK file.")
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    fun getInstalledAppInfo(app: AppBase): Signature {
        if (DeviceSdkTester.supportsAndroid9()) {
            packageManager.getPackageInfo(app.packageName, GET_SIGNING_CERTIFICATES)
                ?.let { extractSignatureFromSigningInfo(it) }
                ?.let { return it }
        }

        packageManager.getPackageInfo(app.packageName, GET_SIGNATURES)
            ?.let { extractSignatureFromSignatures(it) }
            ?.let { return it }

        throw IllegalArgumentException("Can't extract the signature from app.")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractSignatureFromSigningInfo(packageInfo: PackageInfo?): Signature? {
        val signingInfo = packageInfo?.signingInfo ?: return null
        check(!signingInfo.hasMultipleSigners()) { "Multiple signers are not allowed." }
        val signatures = signingInfo.signingCertificateHistory
        check(signatures.isNotEmpty()) { "Signing certificate history must not be empty." }
        check(signatures.size == 1) { "Multiple signatures are not allowed." }
        val signature = signatures[0]
        checkNotNull(signature)
        return signature
    }

    @Suppress("DEPRECATION")
    private fun extractSignatureFromSignatures(packageInfo: PackageInfo?): Signature? {
        val signatures = packageInfo?.signatures ?: return null
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Multiple signatures are not allowed." }
        return signatures[0]
    }

    @MainThread
    suspend fun getPackageArchiveVersionNameOrNull(path: String): String? {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageArchiveInfo(path, 0)?.versionName
        }
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
}