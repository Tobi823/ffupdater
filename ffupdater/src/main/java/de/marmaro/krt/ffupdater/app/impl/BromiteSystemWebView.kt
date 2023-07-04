package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/bromite/bromite/releases
 * https://api.github.com/repos/bromite/bromite/releases
 * https://www.apkmirror.com/apk/bromite/bromite-system-webview-2/
 */
@Deprecated("latest release is too old")
class BromiteSystemWebView : AppBase() {
    override val app = App.BROMITE_SYSTEMWEBVIEW
    override val packageName = "org.bromite.webview"
    override val title = R.string.bromite_systemwebview__title
    override val description = R.string.bromite_systemwebview__description
    override val installationWarning = R.string.bromite__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_bromite_systemwebview
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val eolReason = R.string.eol_reason__browser_no_longer_maintained

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e1ee5cd076d7b0dc84cb2b45fb78b86df2eb39a3b6c56ba3dc292a5e0c3b9504"
    override val installableByUser = false
    override val projectPage = "https://github.com/bromite/bromite"
    override val displayCategory = DisplayCategory.EOL

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        cacheBehaviour: CacheBehaviour,
    ): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileName = findFileName()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.REPOSITORY__BROMITE__BROMITE,
            resultsPerApiCall = GithubConsumer.RESULTS_PER_API_CALL__BROMITE,
            dontUseApiForLatestRelease = true,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name == fileName },
            cacheBehaviour = cacheBehaviour,
        )
        // tag name can be "90.0.4430.59"
        val version = result.tagName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileName(): String {
        val fileName = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "arm_SystemWebView.apk"
            ABI.ARM64_V8A -> "arm64_SystemWebView.apk"
            ABI.X86 -> "x86_SystemWebView.apk"
            ABI.X86_64 -> "x64_SystemWebView.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return fileName
    }

    companion object {
        private const val LOG_TAG = "BromiteSysView"
    }
}