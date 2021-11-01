package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiJsonConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.security.FileHashCalculator
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
class FirefoxNightly : BaseAppWithCachedUpdateCheck() {
    override val packageName = "org.mozilla.fenix"
    override val displayTitle = R.string.firefox_nightly__title
    override val displayDescription = R.string.firefox_nightly__description
    override val displayWarning = R.string.firefox_nightly__warning
    override val displayDownloadSource = R.string.mozilla_ci
    override val displayIcon = R.mipmap.ic_logo_firefox_nightly
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val abiString = getStringForCurrentAbi(
            "armeabi-v7a", "arm64-v8a", "x86",
            "x86_64"
        )
        val result = MozillaCiJsonConsumer(
            task = "mobile.v2.fenix.nightly.latest.$abiString",
            apkArtifact = "public/build/$abiString/target.apk"
        ).updateCheck()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val version = formatter.format(result.releaseDate)
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = null,
            fileHash = result.fileHash
        )
    }

    override suspend fun isCacheFileUpToDate(
        context: Context,
        file: File,
        available: AvailableVersionResult,
    ): Boolean {
        val hash = FileHashCalculator.getSHA256ofFile(file)
        return hash == available.fileHash
    }

    override fun isInstalledVersionUpToDate(
        context: Context,
        available: AvailableVersionResult,
    ): Boolean {
        return try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val installedSha256Hash = preferences.getString(INSTALLED_SHA256_HASH, "unknown")
            val installedVersionCode = preferences.getLong(INSTALLED_VERSION_CODE, -1)
            available.fileHash?.hexValue == installedSha256Hash &&
                    getVersionCode(context) == installedVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun appInstallationCallback(context: Context, available: AvailableVersionResult) {
        try {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(INSTALLED_VERSION_CODE, getVersionCode(context))
                .putString(INSTALLED_SHA256_HASH, available.fileHash?.hexValue)
                .apply()
        } catch (e: PackageManager.NameNotFoundException) {
            throw Exception(
                "app should be installed because this method was called - but the app " +
                        "is not installed", e
            )
        }
    }

    /**
     * @throws PackageManager.NameNotFoundException
     */
    private fun getVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        if (DeviceEnvironment.supportsAndroid9()) {
            return packageInfo.longVersionCode
        }
        @Suppress("DEPRECATION")
        return packageInfo.versionCode.toLong()
    }

    companion object {
        const val INSTALLED_VERSION_CODE = "firefox_nightly_installed_version_code"
        const val INSTALLED_SHA256_HASH = "firefox_nightly_installed_sha256_hash"
    }
}