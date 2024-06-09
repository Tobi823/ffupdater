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
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/M66B/FairEmail/releases
 * https://api.github.com/repos/M66B/FairEmail/releases
 */
@Keep
object FairEmail : AppBase() {
    override val app = App.FAIREMAIL
    override val packageName = "eu.faircode.email"
    override val title = R.string.fairemail__title
    override val description = R.string.fairemail__description
    override val installationWarning = R.string.fairemail__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_fairmail
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "e02067249f5a350e0ec703fe9df4dd682e0291a09f0c2e041050bbe7c064f5c9"
    override val projectPage = "https://github.com/M66B/FairEmail"
    override val displayCategory = listOf(OTHER)
    override val hostnameForInternetCheck = "https://api.github.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("M66B", "FairEmail", 0),
            isValidRelease = { true },
            isSuitableAsset = { it.name.matches("FairEmail-([0-9a-z.]+)-github-release.apk".toRegex()) },
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