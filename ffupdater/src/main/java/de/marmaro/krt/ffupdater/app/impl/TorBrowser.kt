package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException

/**
 * https://www.torproject.org/download/#android
 * https://www.apkmirror.com/apk/the-tor-project/tor-browser/
 */
class TorBrowser(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.torproject.torbrowser"
    override val title = R.string.tor_browser__title
    override val description = R.string.tor_browser__description
    override val installationWarning: Int? = null
    override val downloadSource = "https://www.torproject.org/download"
    override val icon = R.mipmap.ic_logo_tor_browser
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "20061f045e737c67375c17794cfedb436a03cec6bacb7cb9f96642205ca2cec8"
    override val projectPage = "https://www.torproject.org/download/#android"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    override fun getInstalledVersion(context: Context): String? {
        val rawVersion = super.getInstalledVersion(context)
        if (rawVersion != null) {
            val pattern = """\((.+)\)"""
            val match = Regex(pattern).find(rawVersion)
            checkNotNull(match) { "Can't find version with regex pattern '$pattern'." }
            val version = match.groups[1]?.value
            checkNotNull(version) { "Can't extract version from match." }
            return version
        }
        return null
    }

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val (version, downloadUrl) = extractVersionAndDownloadUrl(context)
        val (date, size) = extractDateAndSize(context, version)
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = date,
            fileSizeBytes = size,
            fileHash = null,
            firstReleaseHasAssets = true,
        )
    }

    private suspend fun extractVersionAndDownloadUrl(context: Context): Pair<String, String> {
        val content = try {
            apiConsumer.consumeAsync(MAIN_URL, String::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
        val pattern = """(https://dist\.torproject\.org""" +
                """/torbrowser/(.+)/.+-android-${getAbiString()}-multi\.apk)"""
        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find download url with regex pattern '$pattern'." }
        val downloadUrl = match.groups[1]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }
        val availableVersion = match.groups[2]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }
        return availableVersion.value to downloadUrl.value
    }

    private fun getAbiString(): String {
        return when (deviceAbiExtractor.supportedAbis.firstOrNull { abi -> abi in supportedAbis }) {
            ABI.ARM64_V8A -> "aarch64"
            ABI.ARMEABI_V7A -> "armv7"
            ABI.X86_64 -> "x86_64"
            ABI.X86 -> "x86"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }

    private suspend fun extractDateAndSize(context: Context, version: String): Pair<String, Long> {
        val url = "https://dist.torproject.org/torbrowser/$version/?P=*android-${getAbiString()}-multi.apk"
        val content = try {
            apiConsumer.consumeAsync(url, String::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
        val pattern = """</a>[ ]*([0-9\-]{10}) ([0-9:]{5})[ ]*([0-9:]+)M( )*\n"""
        val match = Regex(pattern).find(content)
        checkNotNull(match) { "Can't find creation date or size with regex pattern '$pattern'." }
        val date = match.groups[1]
        checkNotNull(date) { "Can't extract date from regex match." }
        val time = match.groups[2]
        checkNotNull(time) { "Can't extract time from regex match." }
        val size = match.groups[3]
        checkNotNull(size) { "Can't extract size from regex match." }
        val sizeBytes = (size.value.toLong() + 1) * 1024 * 1024
        return "${date.value}T${time.value}:00Z" to sizeBytes
    }

    companion object {
        private const val LOG_TAG = "Tor Browser"
        const val MAIN_URL = "https://www.torproject.org/download"
    }
}