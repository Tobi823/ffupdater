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
 * https://archive.mozilla.org/pub/fenix/nightly/2024/05/2024-05-20-21-46-33-fenix-128.0a1-android-arm64-v8a/
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
@Keep
object FirefoxNightly : AppBase() {
    override val app = App.FIREFOX_NIGHTLY
    override val packageName = "org.mozilla.fenix"
    override val title = R.string.firefox_nightly__title
    override val description = R.string.firefox_nightly__description
    override val installationWarning = R.string.generic_app_warning__beta_version
    override val downloadSource = "Mozilla CI"
    override val icon = R.drawable.ic_logo_firefox_nightly
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP

    override val signatureHash = "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211"
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val projectPage = "https://www.mozilla.org/firefox/browsers/mobile/android/"
    override val displayCategory = listOf(FROM_MOZILLA)
    override val hostnameForInternetCheck = "https://archive.mozilla.org"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val abi = DeviceAbiExtractor.findBestAbiAsStringA(supportedAbis, DeviceSettingsHelper.prefer32BitApks)
        val page = findPageUrl(abi)
        val version = Regex("""fenix-(\d+\.\d+a\d+)-android""").find(page)!!.groups[1]!!.value
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

    private suspend fun findPageUrl(abi: String): String {
        val hostname = "https://archive.mozilla.org"
        // https://archive.mozilla.org/pub/fenix/nightly/2024/
        val page1 = MozillaArchiveConsumer.findLastLink("https://archive.mozilla.org/pub/fenix/nightly/")
        // https://archive.mozilla.org/pub/fenix/nightly/2024/05
        val page2 = MozillaArchiveConsumer.findLastLink(hostname + page1)
        // https://archive.mozilla.org/pub/fenix/nightly/2024/05/2024-05-20-21-46-33-fenix-128.0a1-android/
        val page3 = MozillaArchiveConsumer.findLastLink(hostname + page2)
        val page4 = page3.removeSuffix("/")
        // https://archive.mozilla.org/pub/fenix/nightly/2024/05/2024-05-20-21-46-33-fenix-128.0a1-android-arm64-v8a/
        val page5 = "$hostname$page4-$abi/"
        return page5
    }
}