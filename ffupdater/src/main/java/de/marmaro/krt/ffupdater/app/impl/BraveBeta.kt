package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BETTER_THAN_GOOGLE_CHROME
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/brave/brave-browser/releases
 * https://api.github.com/repos/brave/brave-browser/releases
 * https://www.apkmirror.com/apk/brave-software/brave-browser-beta/
 */
@Keep
object BraveBeta : AppBase() {
    override val app = App.BRAVE_BETA
    override val packageName = "com.brave.browser_beta"
    override val title = R.string.brave_beta__title
    override val description = R.string.brave_beta__description
    override val installationWarning = R.string.brave__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_brave_beta
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"
    override val projectPage = "https://github.com/brave/brave-browser"
    override val displayCategory = listOf(BETTER_THAN_GOOGLE_CHROME)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val fileName = findNameOfApkFile()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.REPOSITORY__BRAVE__BRAVE_BROWSER,
            resultsPerApiCall = GithubConsumer.RESULTS_PER_API_CALL__BRAVE_BROWSER,
            dontUseApiForLatestRelease = true,
            isValidRelease = { !it.isPreRelease && it.name.startsWith("Beta v") },
            isSuitableAsset = { it.name == fileName },
            cacheBehaviour = cacheBehaviour,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName.replace("v", ""),
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findNameOfApkFile(): String {
        val fileName = if (DeviceSdkTester.supportsAndroid7Nougat24()) {
            when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
                ABI.ARMEABI_V7A -> "BraveMonoarm.apk"
                ABI.ARM64_V8A -> "BraveMonoarm64.apk"
                ABI.X86 -> "BraveMonox86.apk"
                ABI.X86_64 -> "BraveMonox64.apk"
                else -> throw IllegalArgumentException("ABI for Android 7+ is not supported")
            }
        } else {
            when (DeviceAbiExtractor.findBestAbi(ARM32_X86, DeviceSettingsHelper.prefer32BitApks)) {
                ABI.ARMEABI_V7A -> "Bravearm.apk"
                ABI.X86 -> "Bravex86.apk"
                else -> throw IllegalArgumentException("ABI for Android 6 is not supported")
            }
        }
        return fileName
    }
}