package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
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
 * https://github.com/ungoogled-software/ungoogled-chromium-android/releases
 */
@Keep
@Deprecated("app is no longer supported")
class UngoogledChromium : AppBase() {
    override val app = App.UNGOOGLED_CHROMIUM
    override val packageName = "org.ungoogled.chromium.stable"
    override val title = R.string.ungoogled_chromium__title
    override val description = R.string.ungoogled_chromium__description
    override val installationWarning = R.string.ungoogled_chromium__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_ungoogled_chromium
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86)
    override val projectPage = "https://github.com/ungoogled-software/ungoogled-chromium-android"

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "7e6ba7bbb939fa52d5569a8ea628056adf8c75292bf4dee6b353fafaf2c30e19"
    override val eolReason = R.string.ungoogled_chromium__eol_reason
    override val displayCategory = DisplayCategory.EOL

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestUpdate {
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("ungoogled-software", "ungoogled-chromium-android"),
            resultsPerApiCall = 2,
            isValidRelease = { !it.isPreRelease && "webview" !in it.name },
            isSuitableAsset = { it.name == findFileName() },
            dontUseApiForLatestRelease = true,
            cacheBehaviour = cacheBehaviour,
        )
        return LatestUpdate(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null
        )
    }

    private fun findFileName(): String {
        val fileName = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "ChromeModernPublic_arm.apk"
            ABI.ARM64_V8A -> "ChromeModernPublic_arm64.apk"
            ABI.X86 -> "ChromeModernPublic_x86.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return fileName
    }

    companion object {
        private const val LOG_TAG = "UngooChromium"
    }
}