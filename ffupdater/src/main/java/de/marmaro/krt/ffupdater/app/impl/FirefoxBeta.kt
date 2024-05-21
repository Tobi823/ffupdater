package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.FROM_MOZILLA
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.website.MozillaArchiveConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://archive.mozilla.org/pub/fenix/releases/107.0b6/android/fenix-107.0b6-android-arm64-v8a/
 * https://www.apkmirror.com/apk/mozilla/firefox-beta
 */
@Keep
object FirefoxBeta : AppBase() {
    override val app = App.FIREFOX_BETA
    override val packageName = "org.mozilla.firefox_beta"
    override val title = R.string.firefox_beta__title
    override val description = R.string.firefox_beta__description
    override val installationWarning = R.string.firefox_beta__warning
    override val downloadSource = "Github"
    override val icon = R.drawable.ic_logo_firefox_beta
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    override val signatureHash = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"
    override val projectPage = "https://www.mozilla.org/firefox/browsers/mobile/android/"
    override val displayCategory = listOf(FROM_MOZILLA)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val version = findLatestVersion(cacheBehaviour)
        val abi = DeviceAbiExtractor.findBestAbiAsStringA(supportedAbis, DeviceSettingsHelper.prefer32BitApks)
        val page = "https://archive.mozilla.org/pub/fenix/releases/$version/android/fenix-$version-android-$abi/"
        val downloadUrl = "${page}fenix-$version.multi.android-$abi.apk"
        val dateTime = MozillaArchiveConsumer.findDateTimeFromPage(page, cacheBehaviour)
        return LatestVersion(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = dateTime.toString(),
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    private suspend fun findLatestVersion(cacheBehaviour: CacheBehaviour): String {
        val url = "https://archive.mozilla.org/pub/fenix/releases/"
        val versionRegex = Regex("""(\d+)\.(\d+b\d+)""")
        val version = MozillaArchiveConsumer.findLatestVersion(url, versionRegex, cacheBehaviour)
        return version
    }
}