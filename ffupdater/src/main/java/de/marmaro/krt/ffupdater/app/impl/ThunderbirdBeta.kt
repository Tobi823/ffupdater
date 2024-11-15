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
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.website.MozillaArchiveConsumer

/**
 * https://github.com/k9mail/k9mail.app
 * https://github.com/thunderbird/thunderbird-android/releases
 * https://api.github.com/repos/thunderbird/thunderbird-android/releases
 */
@Keep
object ThunderbirdBeta : AppBase() {
    override val app = App.THUNDERBIRD_BETA
    override val packageName = "net.thunderbird.android.beta"
    override val title = R.string.thunderbird_beta__title
    override val description = R.string.thunderbird__description
    override val installationWarning = R.string.generic_beta__warning
    override val downloadSource = "Mozilla Archive"
    override val icon = R.drawable.ic_logo_thunderbird_beta
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "056bfafb450249502fd9226228704c2529e1b822da06760d47a85c9557741fbd"
    override val projectPage = "https://github.com/thunderbird/thunderbird-android"
    override val displayCategory = listOf(FROM_MOZILLA)
    override val hostnameForInternetCheck = "https://archive.mozilla.org"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val version = findLatestVersion()
        val page = "https://archive.mozilla.org/pub/thunderbird-mobile/android/releases/$version/"
        val downloadUrl = "${page}thunderbird-$version.apk"
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
        val url = "https://archive.mozilla.org/pub/thunderbird-mobile/android/releases/"
        val versionRegex = Regex("""(\d+)\.(\d+b\d+)""")
        val version = MozillaArchiveConsumer.findLatestVersion(url, versionRegex)
        return version
    }
}