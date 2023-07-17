package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.*
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://www.torproject.org/download/alpha/
 * https://www.apkmirror.com/apk/the-tor-project/tor-browser-for-android-alpha/
 */
@Keep
object TorBrowserAlpha : AppBase() {
    override val app = App.TOR_BROWSER_ALPHA
    override val packageName = "org.torproject.torbrowser_alpha"
    override val title = R.string.tor_browser_alpha__title
    override val description = R.string.tor_browser_alpha__description
    override val installationWarning = R.string.generic_app_warning__beta_version
    override val downloadSource = "https://www.torproject.org/download/alpha/"
    override val icon = R.drawable.ic_logo_tor_browser_alpha
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "15f760b41acbe4783e667102c9f67119be2af62fab07763f9d57f01e5e1074e1"
    override val projectPage = "https://www.torproject.org/download/alpha/"
    override val displayCategory = listOf(BASED_ON_FIREFOX, GOOD_PRIVACY_BROWSER, GOOD_SECURITY_BROWSER)

    override suspend fun getInstalledVersion(packageManager: PackageManager): String? {
        val rawVersion = super.getInstalledVersion(packageManager) ?: return null
        val rightPart = rawVersion.split(" ")[1]
        return rightPart.trim { it in listOf('(', ')') }
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

    private suspend fun findVersionAndDownloadUrl(cacheBehaviour: CacheBehaviour): Pair<String, String> {
        val content = FileDownloader.downloadStringWithCache(MAIN_URL, cacheBehaviour)
        val pattern = Regex.escape("https://dist.torproject.org/torbrowser/") +
                "([0-9a-z.]{4,})" +
                Regex.escape("/tor-browser-") +
                "[0-9a-z.]{4,}+" +
                Regex.escape("-android-${getAbiString()}-multi.apk")

        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find download url with regex pattern: $pattern." }

        val downloadUrl = match.groups[0]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }

        val availableVersion = match.groups[1]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }

        return availableVersion.value to downloadUrl.value
    }

    private suspend fun findDateTime(version: String, cacheBehaviour: CacheBehaviour): String {
        val url = "https://dist.torproject.org/torbrowser/$version/?P=*android-${getAbiString()}-multi.apk"
        val content = FileDownloader.downloadStringWithCache(url, cacheBehaviour)
        val spaces = """\s+"""
        val pattern = Regex.escape("</a>") +
                spaces +
                """(\d{4}-\d{1,2}-\d{1,2}) """ + //for example 2022-12-16
                """(\d{1,2}:\d{1,2})""" + //for example 13:30
                spaces +
                """(\d){2,3}M""" + //for 82M
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

    private const val MAIN_URL = "https://www.torproject.org/download/alpha/"
}