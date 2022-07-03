package de.marmaro.krt.ffupdater.app.maintained

import android.os.Build
import android.util.Log
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer

/**
 * https://vivaldi.com/de/download/
 * https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/
 */
class Vivaldi(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "com.vivaldi.browser"
    override val displayTitle = R.string.vivaldi__title
    override val displayDescription = R.string.vivaldi__description
    override val displayWarning = R.string.vivaldi__warning
    override val displayDownloadSource = R.string.vivaldi__source
    override val displayIcon = R.mipmap.ic_logo_vivaldi
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.ARMEABI, ABI.X86_64)
    override val normalInstallation = true

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e8a78544655ba8c09817f732768f5689b1662ec4b2bc5a0bc0ec138d33ca3d1e"

    override suspend fun checkForUpdate(): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val abi = deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }

        val content = apiConsumer.consumeAsync(DOWNLOAD_WEBSITE_URL, String::class).await()

        val (version, downloadUrl) = extractVersion(content, abi)
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = null,
            fileSizeBytes = null,
            fileHash = null,
            firstReleaseHasAssets = true,
        )
    }

    private fun extractVersion(content: String, abi: ABI): Pair<String, String> {
        val regexPattern = when (abi) {
            ABI.ARMEABI_V7A -> """<a href="(https://downloads.vivaldi.com/stable/Vivaldi.([.0-9]{1,24})_armeabi-v7a.apk)""""
            ABI.ARM64_V8A -> """<a href="(https://downloads.vivaldi.com/stable/Vivaldi.([.0-9]{1,24})_arm64-v8a.apk)""""
            ABI.X86_64 -> """<a href="(https://downloads.vivaldi.com/stable/Vivaldi.([.0-9]{1,24})_x86-64.apk)""""
            else -> throw IllegalArgumentException("$abi is not supported")
        }

        val regexMatch = Regex(regexPattern).find(content)
        checkNotNull(regexMatch) { "Can't find download link with regex pattern '$regexPattern'." }

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