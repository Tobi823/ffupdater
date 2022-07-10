package de.marmaro.krt.ffupdater.app.maintained

import android.net.Uri
import android.os.Build
import android.util.Log
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/brave/brave-browser/releases
 * https://api.github.com/repos/brave/brave-browser/releases
 * https://www.apkmirror.com/apk/brave-software/brave-browser-beta/
 */
class BraveBeta(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "com.brave.browser_beta"
    override val displayTitle = R.string.brave_beta__title
    override val displayDescription = R.string.brave_beta__description
    override val displayWarning = R.string.brave__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_brave_beta
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)
    override val normalInstallation = true
    override val projectPage: Uri = Uri.parse("https://github.com/brave/brave-browser")

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"

    override suspend fun findLatestUpdate(): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileName = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "BraveMonoarm.apk"
            ABI.ARM64_V8A -> "BraveMonoarm64.apk"
            ABI.X86 -> "BraveMonox86.apk"
            ABI.X86_64 -> "BraveMonox64.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "brave",
            repoName = "brave-browser",
            resultsPerPage = 20,
            isValidRelease = { release -> !release.isPreRelease && release.name.startsWith("Beta v") },
            isCorrectAsset = { asset -> asset.name == fileName },
            dontUseApiForLatestRelease = true,
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()
        val version = result.tagName.replace("v", "")
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }

    companion object {
        private const val LOG_TAG = "BraveBeta"
    }
}