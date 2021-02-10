package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.UpdateCheckResult.Companion.FILE_HASH_SHA256
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-beta/
 */
class FirefoxBeta(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.mozilla.firefox_beta"
    override val displayTitle = R.string.firefox_beta_title
    override val displayDescription = R.string.firefox_beta_description
    override val displayWarning = R.string.firefox_beta_warning
    override val displayDownloadSource = R.string.mozilla_ci
    override val signatureHash = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.AARCH64, ABI.ARM, ABI.X86_64, ABI.X86)

    override fun getDisplayInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override fun getInstalledVersion(context: Context): String? {
        return getInstalledVersionFromSharedPreferences(context, INSTALLED_VERSION_KEY)
    }

    override fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment): UpdateCheckResult {
        check(deviceEnvironment.abis.isNotEmpty())
        val abiString = when (deviceEnvironment.abis[0]) {
            ABI.AARCH64 -> "arm64-v8a"
            ABI.ARM -> "armeabi-v7a"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x86_64"
        }
        val mozillaCiConsumer = MozillaCiConsumer(
                apiConsumer = apiConsumer,
                task = "mobile.v2.fenix.beta.latest.$abiString",
                apkArtifact = "build/$abiString/target.apk")
        val result = mozillaCiConsumer.updateCheck()
        val version = result.timestamp
        val updateAvailable = getInstalledVersion(context) != version
        return UpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                downloadUrl = result.url,
                version = version,
                metadata = mapOf(FILE_HASH_SHA256 to result.hash))
    }

    override fun installationCallback(context: Context, installedVersion: String) {
        setInstalledVersionInSharedPreferences(context, INSTALLED_VERSION_KEY, installedVersion)
    }

    companion object {
        const val INSTALLED_VERSION_KEY = "device_app_register_FIREFOX_BETA_version_name"
    }
}