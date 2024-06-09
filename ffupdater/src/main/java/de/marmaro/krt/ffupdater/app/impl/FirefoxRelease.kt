package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.FROM_MOZILLA
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.website.MozillaArchiveConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://archive.mozilla.org/pub/fenix/releases/126.0/android/fenix-126.0-android-arm64-v8a/
 * https://www.apkmirror.com/apk/mozilla/firefox/
 */
@Keep
object FirefoxRelease : AppBase() {
    override val app = App.FIREFOX_RELEASE
    override val packageName = "org.mozilla.firefox"
    override val title = R.string.firefox_release__title
    override val description = R.string.firefox_release__description
    override val installationWarning = R.string.firefox_release__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_firefox_release
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    override val signatureHash = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"
    override val projectPage = "https://www.mozilla.org/firefox/browsers/mobile/android/"
    override val displayCategory = listOf(FROM_MOZILLA)
    override val hostnameForInternetCheck = "https://archive.mozilla.org"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val version = findLatestVersion()
        val abi = DeviceAbiExtractor.findBestAbiAsStringA(supportedAbis, DeviceSettingsHelper.prefer32BitApks)
        val page = "https://archive.mozilla.org/pub/fenix/releases/$version/android/fenix-$version-android-$abi/"
        val downloadUrl = "${page}fenix-$version.multi.android-$abi.apk"
        val dateTime = MozillaArchiveConsumer.findDateTimeFromPage(page)
        return LatestVersion(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = dateTime.toString(),
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    private suspend fun findLatestVersion(): String {
        val url = "https://archive.mozilla.org/pub/fenix/releases/"
        val versionRegex = Regex("""(\d+)\.(\d+)\.?(\d+)?""")
        val version = MozillaArchiveConsumer.findLatestVersion(url, versionRegex)
        return version
    }
}
