package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/brave/brave-browser/releases
 * https://api.github.com/repos/brave/brave-browser/releases
 * https://www.apkmirror.com/apk/brave-software/brave-browser/
 */
class Brave(
    private val failIfValidReleaseHasNoValidAsset: Boolean = false
) : BaseAppWithCachedUpdateCheck() {
    override val packageName = "com.brave.browser"
    override val displayTitle = R.string.brave__title
    override val displayDescription = R.string.brave__description
    override val displayWarning = R.string.brave__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_brave
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val fileName = getStringForCurrentAbi(
            "BraveMonoarm.apk", "BraveMonoarm64.apk",
            "BraveMonox86.apk", "BraveMonox64.apk"
        )
        val githubConsumer = GithubConsumer(
            repoOwner = "brave",
            repoName = "brave-browser",
            resultsPerPage = 20,
            isValidRelease = { release -> release.name.startsWith("Release v") },
            isCorrectAsset = { asset -> asset.name == fileName },
            failIfValidReleaseHasNoValidAsset = failIfValidReleaseHasNoValidAsset,
            onlyRequestReleasesInBulk = true,
        )
        val result = githubConsumer.updateCheck()
        val version = result.tagName.replace("v", "")
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }
}