package de.marmaro.krt.ffupdater.app.maintained

import android.content.Context
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
 * https://github.com/fork-maintainers/iceraven-browser
 * https://api.github.com/repos/fork-maintainers/iceraven-browser/releases
 */
class Iceraven(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "io.github.forkmaintainers.iceraven"
    override val displayTitle = R.string.iceraven__title
    override val displayDescription = R.string.iceraven__description
    override val displayWarning = R.string.iceraven__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_iceraven
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)
    override val normalInstallation = true
    override val projectPage: Uri = Uri.parse("https://github.com/fork-maintainers/iceraven-browser")

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c0d22379f487b70a4f9f8bec0173cf91a1644f08f93385b5b782ce37660ba81"

    override fun getInstalledVersion(context: Context): String? {
        val installedVersion = super.getInstalledVersion(context)
        return installedVersion?.replace("iceraven-", "")
    }

    override suspend fun findLatestUpdate(): LatestUpdate {
        TODO("Iceraven is currently outdated and should not be used")
        Log.d(LOG_TAG, "check for latest version")
        val fileSuffix = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "browser-armeabi-v7a-forkRelease.apk"
            ABI.ARM64_V8A -> "browser-arm64-v8a-forkRelease.apk"
            ABI.X86 -> "browser-x86-forkRelease.apk"
            ABI.X86_64 -> "browser-x86_64-forkRelease.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "fork-maintainers",
            repoName = "iceraven-browser",
            resultsPerPage = 3,
            isValidRelease = { release -> !release.isPreRelease },
            isCorrectAsset = { asset -> asset.name.endsWith(fileSuffix) },
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()
        val version = result.tagName.replace("iceraven-", "")
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
            fileHash = null,
        )
    }

    companion object {
        private const val LOG_TAG = "Iceraven"
    }
}