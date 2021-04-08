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
 * https://github.com/bromite/bromite/releases
 * https://api.github.com/repos/bromite/bromite/releases
 * https://www.apkmirror.com/apk/bromite/bromite/
 */
class Bromite(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.bromite.bromite"
    override val displayTitle = R.string.bromite__title
    override val displayDescription = R.string.bromite__description
    override val displayWarning = R.string.bromite__warning
    override val displayDownloadSource = R.string.github
    override val signatureHash = "e1ee5cd076d7b0dc84cb2b45fb78b86df2eb39a3b6c56ba3dc292a5e0c3b9504"
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86)

    override suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment): UpdateCheckSubResult {
        val fileName = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> "arm64_ChromePublic.apk"
                ABI.ARMEABI_V7A -> "arm_ChromePublic.apk"
                ABI.X86 -> "x86_ChromePublic.apk"
                ABI.X86_64, ABI.ARMEABI, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
        val githubConsumer = GithubConsumer(
                apiConsumer = apiConsumer,
                repoOwner = "bromite",
                repoName = "bromite",
                resultsPerPage = 5,
                validReleaseTester = { release: Release ->
                    !release.isPreRelease &&
                            release.name.startsWith("Bromite v") &&
                            release.assets.any { it.name == fileName }
                },
                correctDownloadUrlTester = { asset: Asset -> asset.name == fileName })
        val result = githubConsumer.updateCheck()
        return UpdateCheckSubResult(
                downloadUrl = result.url,
                version = result.tagName,
                publishDate = result.releaseDate,
                fileSizeBytes = result.fileSizeBytes)
    }
}