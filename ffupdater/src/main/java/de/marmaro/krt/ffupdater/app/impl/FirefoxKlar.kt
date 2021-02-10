package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-klar-the-privacy-browser-2/
 */
class FirefoxKlar(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.mozilla.klar"
    override val displayTitle = R.string.firefox_klar_title
    override val displayDescription = R.string.firefox_klar_description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.mozilla_ci
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.AARCH64, ABI.ARM)

    override fun getDisplayInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override fun getInstalledVersion(context: Context): String? {
        return getInstalledVersionFromSharedPreferences(context, INSTALLED_VERSION_KEY)
    }

    override fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment): UpdateCheckResult {
        val abiString = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.AARCH64 -> "aarch64"
                ABI.ARM -> "arm"
                ABI.X86, ABI.X86_64 -> null
            }
        }.first()
        val mozillaCiConsumer = MozillaCiConsumer(
                apiConsumer = apiConsumer,
                task = "project.mobile.focus.release.latest",
                apkArtifact = "app-klar-$abiString-release-unsigned.apk")
        val result = mozillaCiConsumer.updateCheck()
        val version = result.timestamp
        val updateAvailable = getInstalledVersion(context) != version
        return UpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                downloadUrl = result.url,
                version = version,
                metadata = mapOf(UpdateCheckResult.FILE_HASH_SHA256 to result.hash))
    }

    override fun installationCallback(context: Context, installedVersion: String) {
        setInstalledVersionInSharedPreferences(context, INSTALLED_VERSION_KEY, installedVersion)
    }

    companion object {
        const val INSTALLED_VERSION_KEY = "device_app_register_FIREFOX_KLAR_version_name"
    }
}