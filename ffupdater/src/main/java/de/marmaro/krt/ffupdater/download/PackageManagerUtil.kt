package de.marmaro.krt.ffupdater.download

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PackageManagerUtil {

    @MainThread
    suspend fun getPackageArchiveInfo(packageManager: PackageManager, path: String): Signature? {
        return withContext(Dispatchers.IO) {
            if (DeviceEnvironment.supportsAndroid11()) {
                // GET_SIGNING_CERTIFICATES does not work on Android 9/10
                val packageInfo = packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)
                extractSignatureForNewerDevices(packageInfo)
            } else {
                val packageInfo = packageManager.getPackageArchiveInfo(path, GET_SIGNATURES)
                extractSignatureForOlderDevices(packageInfo)
            }
        }
    }

    @MainThread
    suspend fun getPackageArchiveVersionName(packageManager: PackageManager, path: String): String? {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageArchiveInfo(path, 0)?.versionName
        }
    }

    fun getInstalledAppInfo(packageManager: PackageManager, app: App): Signature? {
        return if (DeviceEnvironment.supportsAndroid9()) {
            val packageInfo = packageManager.getPackageInfo(app.detail.packageName, GET_SIGNING_CERTIFICATES)
            extractSignatureForNewerDevices(packageInfo)
        } else {
            @SuppressLint("PackageManagerGetSignatures")
            // because GET_SIGNATURES is dangerous on Android 4.4 or lower https://stackoverflow.com/a/39348300
            val packageInfo = packageManager.getPackageInfo(app.detail.packageName, GET_SIGNATURES)
            extractSignatureForOlderDevices(packageInfo)
        }
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractSignatureForNewerDevices(packageInfo: PackageInfo?): Signature? {
        val signingInfo = (packageInfo ?: return null).signingInfo
        checkNotNull(signingInfo) { "PackageInfo#signingInfo is null" }
        check(!signingInfo.hasMultipleSigners()) { "App ${packageInfo.packageName} has multiple signers" }
        check(signingInfo.signingCertificateHistory.size == 1) {
            "App ${packageInfo.packageName} has ${signingInfo.signingCertificateHistory.size} certificates"
        }
        return signingInfo.signingCertificateHistory[0]
    }

    private fun extractSignatureForOlderDevices(packageInfo: PackageInfo?): Signature? {
        val signatures = (packageInfo ?: return null).signatures
        checkNotNull(signatures) { "PackageInfo#signatures is null" }
        check(signatures.size == 1) { "App ${packageInfo.packageName} has ${signatures.size} signatures" }
        return signatures[0]
    }
}