package de.marmaro.krt.ffupdater.security

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class PackageManagerUtil(
    private val packageManager: PackageManager,
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) {

    @Suppress("DEPRECATION")
    @MainThread
    @Throws(FileNotFoundException::class)
    fun getPackageArchiveInfo(path: String): Signature {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File '$path' does not exists.")
        }

//        TODO
//        if (deviceSdkTester.supportsAndroid13()) {
//              https://developer.android.com/reference/android/content/pm/PackageManager#getPackageArchiveInfo(java.lang.String,%20android.content.pm.PackageManager.PackageInfoFlags)
//        }

        if (deviceSdkTester.supportsAndroid9()) {
            packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)
                ?.signingInfo
                ?.let { return extractSignature(it) }
        }

        packageManager.getPackageArchiveInfo(path, GET_SIGNATURES)
            ?.signatures
            ?.let { return extractSignature(it) }

        throw IllegalArgumentException(
            "Can't extract the signature from APK file '$path', " +
                    "length: ${file.length()}, absolutePath: ${file.absolutePath}, isFile: ${file.isFile}"
        )
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    fun getInstalledAppInfo(app: AppBase): Signature {
        try {
            if (deviceSdkTester.supportsAndroid9()) {
                packageManager.getPackageInfo(app.packageName, GET_SIGNING_CERTIFICATES)
                    ?.signingInfo
                    ?.let { return extractSignature(it) }
            }

            packageManager.getPackageInfo(app.packageName, GET_SIGNATURES)
                ?.signatures
                ?.let { return extractSignature(it) }
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException("app.packageName is not whitelisted in AndroidManifest.xml", e)
        }

        throw IllegalArgumentException("Can't extract the signature from app ${app.packageName}.")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractSignature(signingInfo: SigningInfo): Signature {
        check(!signingInfo.hasMultipleSigners()) { "Multiple signers are not allowed." }
        val signatures = signingInfo.signingCertificateHistory
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Found multiple signatures." }
        val signature = signatures[0]
        checkNotNull(signature)
        return signature
    }

    @Suppress("DEPRECATION")
    private fun extractSignature(signatures: Array<Signature>): Signature {
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Found multiple signatures." }
        return signatures[0]
    }

    @MainThread
    suspend fun getPackageArchiveVersionNameOrNull(path: String): String? {
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(path, 0)?.versionName
        }
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