package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseApp
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.UpdateCheckResult.Companion.FILE_SIZE_BYTES
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.dao.Asset
import de.marmaro.krt.ffupdater.app.impl.fetch.github.dao.Release
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://api.github.com/repos/brave/brave-browser/releases
 */
class Brave : BaseApp() {
    override val packageName = "com.brave.browser"
    override val displayTitle = R.string.brave_title
    override val displayDescription = R.string.brave_description
    override val displayWarning = R.string.brave_warning
    override val displayDownloadSource = R.string.github
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbi = listOf(ABI.AARCH64, ABI.ARM, ABI.X86_64, ABI.X86)

    override fun getDisplayInstalledVersion(context: Context): String? {
        return getInstalledVersion(context)
    }

    override fun getInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override fun updateCheck(context: Context, abi: ABI): UpdateCheckResult {
        @Suppress("SpellCheckingInspection")
        val fileName = when (abi) {
            ABI.AARCH64 -> "BraveMonoarm64.apk"
            ABI.ARM -> "BraveMonoarm.apk"
            ABI.X86 -> "BraveMonox86.apk"
            ABI.X86_64 -> "BraveMonox64.apk"
        }
        val githubConsumer = GithubConsumer(
                apiConsumer = ApiConsumer(),
                repoOwner = "brave",
                repoName = "brave-browser",
                resultsPerPage = 20,
                validReleaseTester = { release: Release ->
                    !release.isPreRelease && release.assets.any { it.name.endsWith(".apk") }
                },
                correctDownloadUrlTester = { asset: Asset -> asset.name == fileName })
        val result = githubConsumer.updateCheck()
        val version = result.tagName.replace("v", "")
        val updateAvailable = getInstalledVersion(context)?.let { it != version} ?: true
        return UpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                downloadUrl = result.url,
                version = version,
                metadata = mapOf(FILE_SIZE_BYTES to result.fileSizeBytes)
        )
    }

    override fun installationCallback(context: Context, installedVersion: String) {}
}