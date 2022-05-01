package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/ungoogled-software/ungoogled-chromium-android/releases
 */

class UngoogledChromium(
    private val failIfValidReleaseHasNoValidAsset: Boolean = false,
    private val apiConsumer: ApiConsumer,
    private val deviceAbis: List<ABI>,
) : BaseAppWithCachedUpdateCheck() {
    override val packageName = "org.ungoogled.chromium.stable"
    override val displayTitle = R.string.ungoogled_chromium__title
    override val displayDescription = R.string.ungoogled_chromium__description
    override val displayWarning = R.string.ungoogled_chromium__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_ungoogled_chromium
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARMEABI_V7A, ABI.ARM64_V8A, ABI.X86)
    override val normalInstallation = true

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "7e6ba7bbb939fa52d5569a8ea628056adf8c75292bf4dee6b353fafaf2c30e19"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val filteredAbis = deviceAbis.filter { it in supportedAbis }
        val fileName = when (filteredAbis.firstOrNull()) {
            ABI.ARMEABI_V7A -> "ChromeModernPublic_arm.apk"
            ABI.ARM64_V8A -> "ChromeModernPublic_arm64.apk"
            ABI.X86 -> "ChromeModernPublic_x86.apk"
            else -> throw IllegalArgumentException("ABI '${filteredAbis.firstOrNull()}' is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "ungoogled-software",
            repoName = "ungoogled-chromium-android",
            resultsPerPage = 2,
            isValidRelease = { release -> !release.isPreRelease && "webview" !in release.name },
            isCorrectAsset = { asset -> asset.name == fileName },
            failIfValidReleaseHasNoValidAsset = failIfValidReleaseHasNoValidAsset,
            dontUseApiForLatestRelease = true,
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()

        val extractVersion = {
            val regexMatch = Regex("""^([.0-9]+)-\d+$""")
                .find(result.tagName)
            checkNotNull(regexMatch) {
                "Fail to extract the version with regex from string '${result.tagName}'."
            }
            val matchGroup = regexMatch.groups[1]
            checkNotNull(matchGroup) {
                "Fail to extract the version value from regex match: '${regexMatch.value}'."
            }
            matchGroup.value
        }
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = extractVersion(),
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }
}