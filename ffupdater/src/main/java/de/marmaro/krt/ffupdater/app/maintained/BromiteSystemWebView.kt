package de.marmaro.krt.ffupdater.app.maintained

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/bromite/bromite/releases
 * https://api.github.com/repos/bromite/bromite/releases
 * https://www.apkmirror.com/apk/bromite/bromite-system-webview-2/
 */
class BromiteSystemWebView(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.bromite.webview"
    override val displayTitle = R.string.bromite_systemwebview__title
    override val displayDescription = R.string.bromite_systemwebview__description
    override val displayWarning = R.string.bromite__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_bromite_systemwebview
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86, ABI.X86_64)
    override val normalInstallation = false

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e1ee5cd076d7b0dc84cb2b45fb78b86df2eb39a3b6c56ba3dc292a5e0c3b9504"

    override suspend fun checkForUpdate(): LatestUpdate {
        val filteredAbis = deviceAbiExtractor.supportedAbis
            .filter { it in supportedAbis }
        val fileName = when (filteredAbis.firstOrNull()) {
            ABI.ARMEABI_V7A -> "arm_SystemWebView.apk"
            ABI.ARM64_V8A -> "arm64_SystemWebView.apk"
            ABI.X86 -> "x86_SystemWebView.apk"
            ABI.X86_64 -> "x64_SystemWebView.apk"
            else -> throw IllegalArgumentException("ABI '${filteredAbis.firstOrNull()}' is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "bromite",
            repoName = "bromite",
            resultsPerPage = 5,
            isValidRelease = { release -> !release.isPreRelease },
            isCorrectAsset = { asset -> asset.name == fileName },
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()
        // tag name can be "90.0.4430.59"
        return LatestUpdate(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }
}