package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://vivaldi.com/de/download/
 * https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/
 */
class Vivaldi : BaseAppWithCachedUpdateCheck() {
    override val packageName = "com.vivaldi.browser"
    override val displayTitle = R.string.vivaldi__title
    override val displayDescription = R.string.vivaldi__description
    override val displayWarning = R.string.vivaldi__warning
    override val displayDownloadSource = R.string.vivaldi__source
    override val displayIcon = R.mipmap.ic_logo_vivaldi
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.ARMEABI, ABI.X86_64)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e8a78544655ba8c09817f732768f5689b1662ec4b2bc5a0bc0ec138d33ca3d1e"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val regexPattern = getStringForCurrentAbi(
            """<a href="(https://downloads.vivaldi.com/stable/Vivaldi.([.0-9]{1,24})_armeabi-v7a.apk)"""",
            """<a href="(https://downloads.vivaldi.com/stable/Vivaldi.([.0-9]{1,24})_arm64-v8a.apk)"""",
            null,
            """<a href="(https://downloads.vivaldi.com/stable/Vivaldi.([.0-9]{1,24})_x86-64.apk)"""",
        )
        val content = ApiConsumer.consumeNetworkResource(DOWNLOAD_WEBSITE_URL, String::class)
        val regexMatch = Regex(regexPattern).find(content)
        checkNotNull(regexMatch) { "Can't find download link with regex pattern '$regexPattern'." }
        val downloadUrl = regexMatch.groups[1]
        checkNotNull(downloadUrl) { "Can't extract download url from regex match." }
        val availableVersion = regexMatch.groups[2]
        checkNotNull(availableVersion) { "Can't extract available version from regex match." }

        return AvailableVersionResult(
            downloadUrl = downloadUrl.value,
            version = availableVersion.value,
            publishDate = null,
            fileSizeBytes = null,
            fileHash = null
        )
    }

    companion object {
        const val DOWNLOAD_WEBSITE_URL = "https://vivaldi.com/download/"
    }
}