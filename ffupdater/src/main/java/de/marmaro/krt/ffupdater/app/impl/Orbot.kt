package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.OTHER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/guardianproject/orbot
 * https://api.github.com/repos/guardianproject/orbot/releases
 * https://www.apkmirror.com/apk/the-tor-project/orbot-proxy-with-tor/
 */
@Keep
object Orbot : AppBase() {
    override val app = App.ORBOT
    override val packageName = "org.torproject.android"
    override val title = R.string.orbot__title
    override val description = R.string.orbot__description
    override val installationWarning: Int? = null
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_orbot
    override val minApiLevel = Build.VERSION_CODES.JELLY_BEAN
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "a454b87a1847a89ed7f5e70fba6bba96f3ef29c26e0981204fe347bf231dfd5b"
    override val projectPage = "https://github.com/guardianproject/orbot"
    override val displayCategory = listOf(OTHER)
    override val hostnameForInternetCheck = "https://api.github.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val assetSuffix = getAssetSuffix()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("guardianproject", "orbot", 0),
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.nameStartsAndEndsWith("Orbot", assetSuffix) },
            cacheBehaviour = cacheBehaviour,
            requireReleaseDescription = false,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun getAssetSuffix(): String {
        return when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "-fullperm-universal-release.apk"
            ABI.ARM64_V8A -> "-fullperm-arm64-v8a-release.apk"
            ABI.X86 -> "-fullperm-universal-release.apk"
            ABI.X86_64 -> "-fullperm-universal-release.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }
}
