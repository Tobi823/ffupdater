package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BASED_ON_FIREFOX
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/BrowserWorks/waterfox-android
 * https://api.github.com/repos/BrowserWorks/waterfox-android/releases
 */
@Keep
object Waterfox : AppBase() {
    override val app = App.WATERFOX
    override val packageName = "net.waterfox.android.release"
    override val title = R.string.waterfox__title
    override val description = R.string.waterfox__description
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_waterfox
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "f091ca3998b49e159d23ee95586999c75d0b7023fcf9c54192a81300ac166e17"
    override val projectPage = "https://github.com/BrowserWorks/waterfox-android"
    override val displayCategory = listOf(BASED_ON_FIREFOX)
    override val hostnameForInternetCheck = "https://api.github.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val fileSuffix = findFileSuffix()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("BrowserWorks", "waterfox-android", 0),
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(fileSuffix) },
            requireReleaseDescription = false,
        )
        val version = result.tagName.substringBefore("-") // result.tagName is "1.1.9-2026016869"
        return LatestVersion(
            downloadUrl = result.url,
            version = Version(version),
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileSuffix(): String {
        val abiString = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A -> "arm64-v8a"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x86_64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return "-$abiString-release.apk"
    }
}
