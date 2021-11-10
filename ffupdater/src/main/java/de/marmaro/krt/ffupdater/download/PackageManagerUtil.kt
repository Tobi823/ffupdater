package de.marmaro.krt.ffupdater.download

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PackageManagerUtil {

    @MainThread
    suspend fun getPackageArchiveInfo(packageManager: PackageManager, path: String): Signature {
        return withContext(Dispatchers.IO) {
            val packageInfo = if (DeviceEnvironment.supportsAndroid11()) {
                // GET_SIGNING_CERTIFICATES does not work on Android 9/10
                val packageInfo = packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)
                checkNotNull(packageInfo) { "PackageInfo for file '$packageInfo' is null." }
            } else {
                val packageInfo = packageManager.getPackageArchiveInfo(path, GET_SIGNATURES)
                checkNotNull(packageInfo) { "PackageInfo for file '$packageInfo' is null." }
            }
            extractSignature(packageInfo)
        }
    }

    @MainThread
    suspend fun getPackageArchiveVersionNameOrNull(packageManager: PackageManager, path: String): String? {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageArchiveInfo(path, 0)?.versionName
        }
    }

    fun getInstalledAppInfo(packageManager: PackageManager, app: App): Signature {
        val packageInfo = if (DeviceEnvironment.supportsAndroid9()) {
            val packageInfo = packageManager.getPackageInfo(app.detail.packageName, GET_SIGNING_CERTIFICATES)
            checkNotNull(packageInfo) { "PackageInfo for package ${app.detail.packageName} is null." }
        } else {
            @SuppressLint("PackageManagerGetSignatures")
            // because GET_SIGNATURES is dangerous on Android 4.4 or lower https://stackoverflow.com/a/39348300
            val packageInfo = packageManager.getPackageInfo(app.detail.packageName, GET_SIGNATURES)
            checkNotNull(packageInfo) { "PackageInfo for package ${app.detail.packageName} is null." }
        }
        return extractSignature(packageInfo)
    }

    fun getInstalledAppVersionName(packageManager: PackageManager, packageName: String): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0)?.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isAppInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun extractSignature(packageInfo: PackageInfo): Signature {
        if (DeviceEnvironment.supportsAndroid9() && packageInfo.signingInfo != null) {
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
        throw IllegalArgumentException("PackageInfo has no signingInfo and no signatures.")
    }
}