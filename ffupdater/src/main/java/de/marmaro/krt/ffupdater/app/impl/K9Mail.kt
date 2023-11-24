package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.OTHER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/k9mail/k9mail.app
 * https://github.com/thunderbird/thunderbird-android/releases
 * https://api.github.com/repos/thunderbird/thunderbird-android/releases
 */
@Keep
object K9Mail : AppBase() {
    override val app = App.K9MAIL
    override val packageName = "com.fsck.k9"
    override val title = R.string.k9mail__title
    override val description = R.string.k9mail__description
    override val installationWarning: Int? = null
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_k9mail
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "55c8a523b97335f5bf60dfe8a9f3e1dde744516d9357e80a925b7b22e4f55524"
    override val projectPage = "https://github.com/k9mail/k9mail.app"
    override val displayCategory = listOf(OTHER)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("thunderbird", "thunderbird-android", 0),
            isValidRelease = { true },
            isSuitableAsset = { it.name.matches("k9-([0-9.]+).apk".toRegex()) },
            cacheBehaviour = cacheBehaviour,
            requireReleaseDescription = false,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }
}