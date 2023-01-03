package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://www.torproject.org/download/alpha/
 * https://www.apkmirror.com/apk/the-tor-project/tor-browser-for-android-alpha/
 */
class TorBrowserAlpha(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.TOR_BROWSER_ALPHA
    override val codeName = "TorBrowserAlpha"
    override val packageName = "org.torproject.torbrowser_alpha"
    override val title = R.string.tor_browser_alpha__title
    override val description = R.string.tor_browser_alpha__description
    override val installationWarning = R.string.generic_app_warning__beta_version
    override val downloadSource = "https://www.torproject.org/download/alpha/"
    override val icon = R.mipmap.ic_logo_tor_browser_alpha
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "15f760b41acbe4783e667102c9f67119be2af62fab07763f9d57f01e5e1074e1"
    override val projectPage = "https://www.torproject.org/download/alpha/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    override fun getInstalledVersion(context: Context): String? {
        val rawVersion = super.getInstalledVersion(context) ?: return null
        val rightPart = rawVersion.split(" ")[1]
        return rightPart.trim { it in listOf('(', ')') }
    }

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val (version, downloadUrl) = extractVersionAndDownloadUrl(networkSettings, deviceSettings)
        val (date, size) = extractDateAndSize(networkSettings, deviceSettings, version)
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = date,
            fileSizeBytes = size,
            fileHash = null,
        )
    }

    private suspend fun extractVersionAndDownloadUrl(
        networkSettings: NetworkSettingsHelper,
        deviceSettings: DeviceSettingsHelper,
    ): Pair<String, String> {
        val content = try {
            apiConsumer.consume(MAIN_URL, networkSettings)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
        val pattern = Regex.escape("https://dist.torproject.org/torbrowser/") +
                "([0-9a-z.]{4,})" +
                Regex.escape("/tor-browser-") +
                "[0-9a-z.]{4,}+" +
                Regex.escape("-android-${getAbiString(deviceSettings)}-multi.apk")

        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find download url with regex pattern: $pattern." }

        val downloadUrl = match.groups[0]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }

        val availableVersion = match.groups[1]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }

        return availableVersion.value to downloadUrl.value
    }

    private suspend fun extractDateAndSize(
        networkSettings: NetworkSettingsHelper,
        deviceSettingsHelper: DeviceSettingsHelper,
        version: String
    ): Pair<String, Long> {
        val abi = getAbiString(deviceSettingsHelper)
        val url = "https://dist.torproject.org/torbrowser/$version/?P=*android-$abi-multi.apk"
        val content = try {
            apiConsumer.consume(url, networkSettings)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }

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

        val size = match.groups[3]
        checkNotNull(size) { "Can't extract size from regex match." }

        val sizeBytes = (size.value.toLong() + 1) * 1024 * 1024
        return "${date.value}T${time.value}:00Z" to sizeBytes
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
        const val MAIN_URL = "https://www.torproject.org/download/alpha/"
    }
}