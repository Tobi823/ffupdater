package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/uazo/cromite
 */
@Keep
object Cromite : AppBase() {
    override val app = App.CROMITE
    override val packageName = "org.cromite.cromite"
    override val title = R.string.cromite__title
    override val description = R.string.cromite__description
    override val installationWarning = R.string.cromite__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.id_logo_cromite
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.X86_64)
    override val signatureHash = "633fa41d8211d6d0916a819b89668c6de92e64232da67f9d16fd81c3b7e923ff"
    override val projectPage = "https://github.com/uazo/cromite"
    override val displayCategory = listOf(DisplayCategory.GOOD_PRIVACY_BROWSER)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val fileName = findFileName()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("uazo", "cromite"),
            resultsPerApiCall = 2,
            dontUseApiForLatestRelease = false,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name == fileName },
            cacheBehaviour = cacheBehaviour,
        )
        val tagNameWithoutPrefix = result.tagName.removePrefix("v")
        val version = tagNameWithoutPrefix.split("-")[0]
        return LatestVersion(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileName(): String {
        val fileName =
            when (DeviceAbiExtractor.findBestAbi(Bromite.supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
                ABI.ARM64_V8A -> "arm64_ChromePublic.apk"
                ABI.X86_64 -> "x64_ChromePublic.apk"
                else -> throw IllegalArgumentException("ABI is not supported")
            }
        return fileName
    }
}