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
object ThunderbirdRelease : AppBase() {
    override val app = App.THUNDERBIRD
    override val packageName = "net.thunderbird.android"
    override val title = R.string.thunderbird__title
    override val description = R.string.thunderbird__description
    override val installationWarning: Int? = null
    override val downloadSource = "Mozilla Archive"
    override val icon = R.drawable.ic_logo_thunderbird
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "b6524779b3dbbc5ac17a5ac271ddb29dcfbf723578c238e03c3c217811356dd1"
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
        val versionRegex = Regex("""(\d+)\.(\d+)\.?(\d+)?""")
        val version = MozillaArchiveConsumer.findLatestVersion(url, versionRegex)
        return version
    }
}