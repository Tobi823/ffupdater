package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.FROM_MOZILLA
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.website.MozillaArchiveConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://archive.mozilla.org/pub/focus/releases/126.0/android/klar-126.0-android-arm64-v8a/
 * https://www.apkmirror.com/apk/mozilla/firefox-klar-the-privacy-browser-2/
 */
@Keep
object FirefoxKlar : AppBase() {
    override val app = App.FIREFOX_KLAR
    override val packageName = "org.mozilla.klar"
    override val title = R.string.firefox_klar__title
    override val description = R.string.firefox_klar__description
    override val downloadSource = "Mozilla Archive"
    override val icon = R.drawable.ic_logo_firefox_focus_klar
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"
    override val projectPage = "https://github.com/mozilla-mobile/firefox-android"
    override val displayCategory = listOf(FROM_MOZILLA)
    override val hostnameForInternetCheck = "https://archive.mozilla.org"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val version = findLatestVersion()
        val abi = DeviceAbiExtractor.findBestAbiAsStringA(supportedAbis, DeviceSettingsHelper.prefer32BitApks)
        val page = "https://archive.mozilla.org/pub/focus/releases/$version/android/klar-$version-android-$abi/"
        val downloadUrl = "${page}klar-$version.multi.android-$abi.apk"
        val dateTime = MozillaArchiveConsumer.findDateTimeFromPage(page)
        return LatestVersion(
            downloadUrl = downloadUrl,
            version = Version(version),
            publishDate = dateTime.toString(),
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    private suspend fun findLatestVersion(): String {
        val url = "https://archive.mozilla.org/pub/focus/releases/"
        val versionRegex = Regex("""(\d+)\.(\d+)\.?(\d+)?""")
        val version = MozillaArchiveConsumer.findLatestVersion(url, versionRegex)
        return version
    }
}