package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BASED_ON_FIREFOX
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/fork-maintainers/iceraven-browser
 * https://api.github.com/repos/fork-maintainers/iceraven-browser/releases
 */
@Keep
object Iceraven : AppBase() {
    override val app = App.ICERAVEN
    override val packageName = "io.github.forkmaintainers.iceraven"
    override val title = R.string.iceraven__title
    override val description = R.string.iceraven__description
    override val installationWarning = R.string.iceraven__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_iceraven
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val signatureHash = "9c0d22379f487b70a4f9f8bec0173cf91a1644f08f93385b5b782ce37660ba81"
    override val projectPage = "https://github.com/fork-maintainers/iceraven-browser"
    override val displayCategory = listOf(BASED_ON_FIREFOX)

    override suspend fun getInstalledVersion(packageManager: PackageManager): String? {
        val installedVersion = super.getInstalledVersion(packageManager)
        return installedVersion?.replace("iceraven-", "")
    }

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val fileSuffix = findFileSuffix()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("fork-maintainers", "iceraven-browser", 0),
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(fileSuffix) },
            cacheBehaviour = cacheBehaviour,
            requireReleaseDescription = false,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName.replace("iceraven-", ""),
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileSuffix(): String {
        val fileSuffix = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "browser-armeabi-v7a-forkRelease.apk"
            ABI.ARM64_V8A -> "browser-arm64-v8a-forkRelease.apk"
            ABI.X86 -> "browser-x86-forkRelease.apk"
            ABI.X86_64 -> "browser-x86_64-forkRelease.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return fileSuffix
    }
}