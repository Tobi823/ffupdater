package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_PRIVACY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_SECURITY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/jqssun/android-titanium-browser
 * https://api.github.com/repos/jqssun/android-titanium-browser/releases
 */
@Keep
object Titanium : AppBase() {
    override val app = App.TITANIUM
    override val packageName = "io.github.jqssun.helium"
    override val title = R.string.titanium__title
    override val description = R.string.titanium__description
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_titanium
    override val minApiLevel = Build.VERSION_CODES.Q
    override val supportedAbis = ARM32_ARM64
    override val signatureHash = "095540494d53f48ca051bc86c392c46edb29725da5592bd8af3c3d027b3166bb"
    override val projectPage = "https://github.com/jqssun/android-titanium-browser"
    override val displayCategory = listOf(GOOD_PRIVACY_BROWSER, GOOD_SECURITY_BROWSER)
    override val hostnameForInternetCheck = "https://api.github.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val fileSuffix = findFileSuffix()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("jqssun", "android-titanium-browser", 0),
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(fileSuffix) },
            requireReleaseDescription = false,
        )
        val version = result.tagName.removePrefix("v")
        return LatestVersion(
            downloadUrl = result.url,
            version = Version(version),
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileSuffix(): String {
        return when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "-armeabi-v7a.apk"
            ABI.ARM64_V8A -> "-arm64-v8a.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }
}
