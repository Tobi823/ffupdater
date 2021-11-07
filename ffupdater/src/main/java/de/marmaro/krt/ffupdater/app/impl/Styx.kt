package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/jamal2362/Styx
 * https://api.github.com/repos/jamal2362/Styx/releases/latest
 */
class Styx : BaseAppWithCachedUpdateCheck() {
    override val packageName = "com.jamal2367.styx"
    override val displayTitle = R.string.styx__title
    override val displayDescription = R.string.styx__description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_styx
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = listOf(ABI.ARMEABI_V7A)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "2e9877a1a1b50bc58b84b88ae4f5713a9e7eeb04d4f62a494816250f66c0dd52"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val githubConsumer = GithubConsumer(
            repoOwner = "jamal2362",
            repoName = "Styx",
            resultsPerPage = 3,
            validReleaseTester = { release: GithubConsumer.Release ->
                !release.isPreRelease && release.assets.any { it.name.endsWith(".apk") }
            },
            correctAssetTester = { asset: GithubConsumer.Asset -> asset.name.endsWith(".apk") })
        val result = githubConsumer.updateCheck()
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }
}