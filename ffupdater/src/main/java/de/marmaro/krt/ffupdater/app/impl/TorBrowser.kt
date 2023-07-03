package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://www.torproject.org/download/#android
 * https://www.apkmirror.com/apk/the-tor-project/tor-browser/
 * https://dist.torproject.org/torbrowser/
 */
class TorBrowser(
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.TOR_BROWSER
    override val packageName = "org.torproject.torbrowser"
    override val title = R.string.tor_browser__title
    override val description = R.string.tor_browser__description
    override val installationWarning: Int? = null
    override val downloadSource = "https://www.torproject.org/download"
    override val icon = R.drawable.ic_logo_tor_browser
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "20061f045e737c67375c17794cfedb436a03cec6bacb7cb9f96642205ca2cec8"
    override val projectPage = "https://www.torproject.org/download/#android"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    override fun getInstalledVersion(context: Context): String? {
        val rawVersion = super.getInstalledVersion(context) ?: return null
        return rawVersion.split(" ").last()
            .removePrefix("(")
            .removeSuffix(")")
    }

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val (version, downloadUrl) = findVersionAndDownloadUrl(fileDownloader, deviceSettings)
        val dateTime = findDateTime(fileDownloader, deviceSettings, version)
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = dateTime,
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    private suspend fun findVersionAndDownloadUrl(
        fileDownloader: FileDownloader,
        deviceSettings: DeviceSettingsHelper,
    ): Pair<String, String> {
        val content = try {
            fileDownloader.downloadSmallFileAsString(MAIN_URL)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
        val pattern = Regex.escape("https://dist.torproject.org/torbrowser/") +
                "([0-9.]{4,})" +
                Regex.escape("/tor-browser-") +
                "[0-9.]{4,}+" +
                Regex.escape("-android-${getAbiString(deviceSettings)}-multi.apk")

        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find download url with regex pattern '$pattern'." }

        val downloadUrl = match.groups[0]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }

        val availableVersion = match.groups[1]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }

        return availableVersion.value to downloadUrl.value
    }

    private suspend fun findDateTime(
        fileDownloader: FileDownloader,
        deviceSettingsHelper: DeviceSettingsHelper,
        version: String,
    ): String {
        val abi = getAbiString(deviceSettingsHelper)
        val url = "https://dist.torproject.org/torbrowser/$version/?P=*android-$abi-multi.apk"
        val content = try {
            fileDownloader.downloadSmallFileAsString(url)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }

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

    private fun getAbiString(settings: DeviceSettingsHelper): String {
        return when (deviceAbiExtractor.findBestAbi(supportedAbis, settings.prefer32BitApks)) {
            ABI.ARM64_V8A -> "aarch64"
            ABI.ARMEABI_V7A -> "armv7"
            ABI.X86_64 -> "x86_64"
            ABI.X86 -> "x86"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }

    companion object {
        private const val LOG_TAG = "Tor Browser"
        const val MAIN_URL = "https://www.torproject.org/download"
    }
}