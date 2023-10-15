package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BASED_ON_FIREFOX
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_PRIVACY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_SECURITY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://www.torproject.org/download/#android
 * https://www.apkmirror.com/apk/the-tor-project/tor-browser/
 * https://dist.torproject.org/torbrowser/
 */
@Keep
object TorBrowser : AppBase() {
    override val app = App.TOR_BROWSER
    override val packageName = "org.torproject.torbrowser"
    override val title = R.string.tor_browser__title
    override val description = R.string.tor_browser__description
    override val installationWarning: Int? = null
    override val downloadSource = "https://www.torproject.org"
    override val icon = R.drawable.ic_logo_tor_browser
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "20061f045e737c67375c17794cfedb436a03cec6bacb7cb9f96642205ca2cec8"
    override val projectPage = "https://www.torproject.org/download/#android"
    override val displayCategory = listOf(BASED_ON_FIREFOX, GOOD_PRIVACY_BROWSER, GOOD_SECURITY_BROWSER)

    override suspend fun getInstalledVersion(packageManager: PackageManager): String? {
        val rawVersion = super.getInstalledVersion(packageManager) ?: return null
        return rawVersion.split(" ").last()
            .removePrefix("(")
            .removeSuffix(")")
    }

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val (version, downloadUrl) = findVersionAndDownloadUrl(cacheBehaviour)
        return LatestVersion(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = findDateTime(version, cacheBehaviour),
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    @Throws(IllegalStateException::class)
    private suspend fun findVersionAndDownloadUrl(cacheBehaviour: CacheBehaviour): Pair<String, String> {
        val content = FileDownloader.downloadStringWithCache(MAIN_URL, cacheBehaviour)
        val pattern = Regex.escape("https://dist.torproject.org/torbrowser/") +
                "([0-9.]{4,})" + // 13.0
                Regex.escape("/tor-browser-android-${getAbiString()}-") + // /tor-browser-android-x86_64-
                "([0-9.]{4,})" + // 13.0
                Regex.escape(".apk")

        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find download url with regex pattern '$pattern'." }

        val downloadUrl = match.groups[0]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }

        val availableVersion = match.groups[1]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }

        return availableVersion.value to downloadUrl.value
    }

    @Throws(IllegalStateException::class)
    private suspend fun findDateTime(version: String, cacheBehaviour: CacheBehaviour): String {
        val url = "https://dist.torproject.org/torbrowser/$version/?P=tor-browser-android-${getAbiString()}-$version.apk"
        val content = FileDownloader.downloadStringWithCache(url, cacheBehaviour)
        val spaces = """\s+"""
        val pattern = Regex.escape("</a>") +
                spaces +
                """(\d{4}-\d{1,2}-\d{1,2}) """ + //for example 2022-12-16
                """(\d{1,2}:\d{1,2})""" + //for example 13:30
                spaces +
                """((\d){2,3})M""" + //for 82M
                spaces +
                """\n"""

        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find creation date or size with regex pattern: $pattern" }

        val date = match.groups[1]
        checkNotNull(date) { "Can't extract date from regex match." }

        val time = match.groups[2]
        checkNotNull(time) { "Can't extract time from regex match." }

        return "${date.value}T${time.value}:00Z"
    }

    private fun getAbiString(): String {
        return when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARM64_V8A -> "aarch64"
            ABI.ARMEABI_V7A -> "armv7"
            ABI.X86_64 -> "x86_64"
            ABI.X86 -> "x86"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }

    private const val MAIN_URL = "https://www.torproject.org/download/"
}