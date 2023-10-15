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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

@Keep
object PackageManagerUtil {

    @Suppress("DEPRECATION")
    @MainThread
    @Throws(FileNotFoundException::class, IllegalStateException::class)
    suspend fun getPackageArchiveInfo(pm: PackageManager, path: String): Signature {
        return withContext(Dispatchers.Default) {
            val file = File(path)
            check(file.exists()) { "File '$path' does not exists." }
            val signatures = mutableListOf<() -> Signature?>()
            if (DeviceSdkTester.supportsAndroid13T33()) {
                signatures.add { extractSignature(pm.getPackageArchiveInfo(path, getPackageInfoFlags())) }
            }
            if (DeviceSdkTester.supportsAndroid9P28()) {
                signatures.add { extractSignature(pm.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)) }
            }
            signatures.add { extractSignature(pm.getPackageArchiveInfo(path, GET_SIGNATURES)) }
            signatures.firstNotNullOf { it() }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    suspend fun getInstalledAppInfo(pm: PackageManager, app: AppBase): Signature {
        return withContext(Dispatchers.Default) {
            try {
                val signatures = mutableListOf<() -> Signature?>()
                if (DeviceSdkTester.supportsAndroid13T33()) {
                    signatures.add { extractSignature(pm.getPackageInfo(app.packageName, getPackageInfoFlags())) }
                }
                if (DeviceSdkTester.supportsAndroid9P28()) {
                    signatures.add { extractSignature(pm.getPackageInfo(app.packageName, GET_SIGNING_CERTIFICATES)) }
                }
                signatures.add { extractSignature(pm.getPackageInfo(app.packageName, GET_SIGNATURES)) }
                signatures.firstNotNullOf { it() }
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException("app.packageName is not whitelisted in AndroidManifest.xml", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPackageInfoFlags(): PackageManager.PackageInfoFlags {
        return PackageManager.PackageInfoFlags.of(GET_SIGNING_CERTIFICATES.toLong())
    }

    @Suppress("DEPRECATION")
    private fun extractSignature(packageInfo: PackageInfo?): Signature? {
        if (DeviceSdkTester.supportsAndroid9P28() && packageInfo?.signingInfo != null) {
            return extractSignature(packageInfo.signingInfo)
        }
        if (packageInfo?.signatures != null) {
            return extractSignature(packageInfo.signatures)
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Throws(IllegalStateException::class)
    private fun extractSignature(signingInfo: SigningInfo): Signature {
        check(!signingInfo.hasMultipleSigners()) { "Multiple signers are not allowed." }
        val signatures = signingInfo.signingCertificateHistory
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Found multiple signatures." }
        return checkNotNull(signatures[0])
    }

    @Throws(IllegalStateException::class)
    private fun extractSignature(signatures: Array<Signature>): Signature {
        check(signatures.isNotEmpty()) { "Signatures must not be empty." }
        check(signatures.size == 1) { "Found multiple signatures." }
        return signatures[0]
    }

    suspend fun getInstalledAppVersionName(pm: PackageManager, packageName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                pm.getPackageInfo(packageName, 0)?.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    suspend fun isAppInstalled(pm: PackageManager, packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}