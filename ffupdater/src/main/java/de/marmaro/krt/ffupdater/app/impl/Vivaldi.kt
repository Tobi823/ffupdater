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
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://vivaldi.com/de/download/
 * https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/
 */
class Vivaldi(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.VIVALDI
    override val packageName = "com.vivaldi.browser"
    override val title = R.string.vivaldi__title
    override val description = R.string.vivaldi__description
    override val installationWarning = R.string.vivaldi__warning
    override val downloadSource = "https://vivaldi.com/download/"
    override val icon = R.drawable.ic_logo_vivaldi
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e8a78544655ba8c09817f732768f5689b1662ec4b2bc5a0bc0ec138d33ca3d1e"
    override val projectPage = "https://vivaldi.com/de/download/"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val content = try {
            apiConsumer.consume(DOWNLOAD_WEBSITE_URL, fileDownloader)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }

        val (version, downloadUrl) = extractVersionAndDownloadUrl(content, deviceSettings)
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = null,
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    private fun extractVersionAndDownloadUrl(
        content: String,
        settings: DeviceSettingsHelper
    ): Pair<String, String> {
        val abiString = when (deviceAbiExtractor.findBestAbi(supportedAbis, settings.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A -> "arm64-v8a"
            ABI.X86_64 -> "x86-64.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val regexPattern = Regex.escape("<a href=\"") +
                "(" +
                Regex.escape("https://downloads.vivaldi.com/stable/Vivaldi.") +
                "([.0-9]{1,24})" +
                Regex.escape("_$abiString.apk") +
                ")\""

        val regexMatch = Regex(regexPattern).find(content)
        checkNotNull(regexMatch) { "Can't find download link with regex pattern: $regexPattern." }

        val downloadUrl = regexMatch.groups[1]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }

        val availableVersion = regexMatch.groups[2]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }

        return availableVersion.value to downloadUrl.value
    }

    companion object {
        private const val LOG_TAG = "Vivaldi"
        const val DOWNLOAD_WEBSITE_URL = "https://vivaldi.com/download/"
    }
}