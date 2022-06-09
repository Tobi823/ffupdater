package de.marmaro.krt.ffupdater.download

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
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
        return extractSignature { flags: Int -> packageManager.getPackageArchiveInfo(path, flags) }
            ?: throw ApkSignatureNotFoundException("Can't extract the signature from the APK file.")
    }

    @MainThread
    suspend fun getPackageArchiveVersionNameOrNull(path: String): String? {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageArchiveInfo(path, 0)?.versionName
        }
    }

    @Throws(ApkSignatureNotFoundException::class)
    fun getInstalledAppInfo(app: BaseApp): Signature {
        return extractSignature { flags: Int -> packageManager.getPackageInfo(app.packageName, flags) }
            ?: throw ApkSignatureNotFoundException("Can't extract the signature from app.")
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

    private fun extractSignature(extractPackageInfo: (flags: Int) -> PackageInfo?): Signature? {
        if (DeviceSdkTester.supportsAndroid9()) {
            extractSignatureForAbi28(extractPackageInfo)
                ?.let { return it }
        }
        // for older devices and fallback for newer devices which don't support the newer way of extracting
        // the signature
        return extractSignatureForOlderDevices(extractPackageInfo)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractSignatureForAbi28(extractPackageInfo: (flags: Int) -> PackageInfo?): Signature? {
        val packageInfo = extractPackageInfo(PackageManager.GET_SIGNING_CERTIFICATES)
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
    private fun extractSignatureForOlderDevices(extractPackageInfo: (flags: Int) -> PackageInfo?): Signature? {
        // because GET_SIGNATURES is dangerous on Android 4.4 or lower https://stackoverflow.com/a/39348300
        @SuppressLint("PackageManagerGetSignatures")
        val packageInfo = extractPackageInfo(PackageManager.GET_SIGNATURES)
        val signatures = packageInfo?.signatures ?: return null

        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Multiple signatures are not allowed." }

        val signature = signatures[0]
        checkNotNull(signature)
        return signature
    }
}