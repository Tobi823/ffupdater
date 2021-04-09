package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.UpdateCheckSubResult
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Asset
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Release
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * https://api.github.com/repos/brave/brave-browser/releases
 */
class Brave(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "com.brave.browser"
    override val displayTitle = R.string.brave__title
    override val displayDescription = R.string.brave__description
    override val displayWarning = R.string.brave__warning
    override val displayDownloadSource = R.string.github
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)

    override suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment): UpdateCheckSubResult {
        val fileName = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> "BraveMonoarm64.apk"
                ABI.ARMEABI_V7A -> "BraveMonoarm.apk"
                ABI.X86 -> "BraveMonox86.apk"
                ABI.X86_64 -> "BraveMonox64.apk"
                ABI.ARMEABI, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
        val githubConsumer = GithubConsumer(
                apiConsumer = apiConsumer,
                repoOwner = "brave",
                repoName = "brave-browser",
                resultsPerPage = 20,
                validReleaseTester = { release: Release ->
                    !release.isPreRelease &&
                            release.name.startsWith("Release v") &&
                            release.assets.any { it.name == fileName }
                },
                correctDownloadUrlTester = { asset: Asset -> asset.name == fileName })
        val result = githubConsumer.updateCheckReliableOnlyForNormalReleases()
        val version = result.tagName.replace("v", "")
        return UpdateCheckSubResult(
                downloadUrl = result.url,
                version = version,
                publishDate = result.releaseDate,
                fileSizeBytes = result.fileSizeBytes)
    }
}