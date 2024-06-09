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
 * https://archive.mozilla.org/pub/focus/releases/127.0b4/android/focus-127.0b4-android-arm64-v8a/
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-beta/
 */
@Keep
object FirefoxFocusBeta : AppBase() {
    override val app = App.FIREFOX_FOCUS_BETA
    override val packageName = "org.mozilla.focus.beta"
    override val title = R.string.firefox_focus_beta__title
    override val description = R.string.firefox_focus_beta__description
    override val installationWarning = R.string.generic_app_warning__beta_version
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_firefox_focus_beta
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
        val page = "https://archive.mozilla.org/pub/focus/releases/$version/android/focus-$version-android-$abi/"
        val downloadUrl = "${page}focus-$version.multi.android-$abi.apk"
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
        val url = "https://archive.mozilla.org/pub/focus/releases/"
        val versionRegex = Regex("""(\d+)\.(\d+b\d+)""")
        val version = MozillaArchiveConsumer.findLatestVersion(url, versionRegex)
        return version
    }
}