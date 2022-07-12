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
 * https://github.com/bromite/bromite/releases
 * https://api.github.com/repos/bromite/bromite/releases
 * https://www.apkmirror.com/apk/bromite/bromite-system-webview-2/
 */
class BromiteSystemWebView(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.bromite.webview"
    override val title = R.string.bromite_systemwebview__title
    override val description = R.string.bromite_systemwebview__description
    override val installationWarning = R.string.bromite__warning
    override val downloadSource = R.string.github
    override val icon = R.mipmap.ic_logo_bromite_systemwebview
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM_AND_X_ABIS
    override val installableWithDefaultPermission = false
    override val projectPage: Uri = Uri.parse("https://github.com/bromite/bromite")

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e1ee5cd076d7b0dc84cb2b45fb78b86df2eb39a3b6c56ba3dc292a5e0c3b9504"

    override suspend fun findLatestUpdate(): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileName = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "arm_SystemWebView.apk"
            ABI.ARM64_V8A -> "arm64_SystemWebView.apk"
            ABI.X86 -> "x86_SystemWebView.apk"
            ABI.X86_64 -> "x64_SystemWebView.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
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
        Log.i(LOG_TAG, "found latest version ${result.tagName}")
        return LatestUpdate(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }

    companion object {
        private const val LOG_TAG = "BromiteSysView"
    }
}