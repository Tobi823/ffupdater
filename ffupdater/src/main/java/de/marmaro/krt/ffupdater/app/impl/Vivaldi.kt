package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BETTER_THAN_GOOGLE_CHROME
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * https://vivaldi.com/de/download/
 * https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/
 */
@Keep
object Vivaldi : AppBase() {
    override val app = App.VIVALDI
    override val packageName = "com.vivaldi.browser"
    override val title = R.string.vivaldi__title
    override val description = R.string.vivaldi__description
    override val installationWarning = R.string.vivaldi__warning
    override val downloadSource = "https://vivaldi.com/download/"
    override val icon = R.drawable.ic_logo_vivaldi
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64)

    override val signatureHash = "e8a78544655ba8c09817f732768f5689b1662ec4b2bc5a0bc0ec138d33ca3d1e"
    override val projectPage = "https://vivaldi.com/de/download/"
    override val displayCategory = listOf(BETTER_THAN_GOOGLE_CHROME)
    override val hostnameForInternetCheck = "https://vivaldi.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val content = FileDownloader.downloadString(DOWNLOAD_WEBSITE_URL)
        val (version, downloadUrl) = extractVersionAndDownloadUrl(content)
        return LatestVersion(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = null,
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    @Throws(IllegalStateException::class)
    private suspend fun extractVersionAndDownloadUrl(content: String): Pair<String, String> {
        return withContext(Dispatchers.Default) {
            val abiString = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
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

            availableVersion.value to downloadUrl.value
        }
    }

    private const val DOWNLOAD_WEBSITE_URL = "https://vivaldi.com/download/"
}