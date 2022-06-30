package de.marmaro.krt.ffupdater.app.maintained

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseApp
import de.marmaro.krt.ffupdater.app.network.ApiConsumer
import de.marmaro.krt.ffupdater.app.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://api.github.com/repos/brave/brave-browser/releases
 */
class FFUpdater(
    private val failIfValidReleaseHasNoValidAsset: Boolean = false,
    private val apiConsumer: ApiConsumer,
) : BaseApp() {
    override val packageName = "de.marmaro.krt.ffupdater"
    override val displayTitle = R.string.app_name
    override val displayDescription = R.string.app_description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_launcher
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis =
        listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.ARMEABI, ABI.X86_64, ABI.X86, ABI.MIPS, ABI.MIPS64)
    override val normalInstallation = false

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "f4e642bb85cbbcfd7302b2cbcbd346993a41067c27d995df492c9d0d38747e62"

    override suspend fun checkForUpdate(): AvailableVersionResult {
        val githubConsumer = GithubConsumer(
            repoOwner = "Tobi823",
            repoName = "ffupdater",
            resultsPerPage = 5,
            isValidRelease = { release -> !release.isPreRelease },
            isCorrectAsset = { asset -> asset.name.endsWith(".apk") },
            failIfValidReleaseHasNoValidAsset = failIfValidReleaseHasNoValidAsset,
            apiConsumer = apiConsumer,
        )
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