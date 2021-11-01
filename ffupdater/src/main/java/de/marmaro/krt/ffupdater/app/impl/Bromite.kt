package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Asset
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Release
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/bromite/bromite/releases
 * https://api.github.com/repos/bromite/bromite/releases
 * https://www.apkmirror.com/apk/bromite/bromite/
 */
class Bromite : BaseAppWithCachedUpdateCheck() {
    override val packageName = "org.bromite.bromite"
    override val displayTitle = R.string.bromite__title
    override val displayDescription = R.string.bromite__description
    override val displayWarning = R.string.bromite__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_bromite
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e1ee5cd076d7b0dc84cb2b45fb78b86df2eb39a3b6c56ba3dc292a5e0c3b9504"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val fileName = getStringForCurrentAbi(
            "arm_ChromePublic.apk",
            "arm64_ChromePublic.apk", "x86_ChromePublic.apk", null
        )
        val githubConsumer = GithubConsumer(
            repoOwner = "bromite",
            repoName = "bromite",
            resultsPerPage = 5,
            validReleaseTester = { release: Release ->
                !release.isPreRelease && release.assets.any { it.name == fileName }
            },
            correctAssetTester = { asset: Asset -> asset.name == fileName })
        val result = githubConsumer.updateCheck()
        // tag name can be "90.0.4430.59"
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }
}