package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI.*
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/brave/brave-browser/releases
 * https://api.github.com/repos/brave/brave-browser/releases
 * https://www.apkmirror.com/apk/brave-software/brave-browser/
 */
class Brave : AppBase() {
    override val app: App = App.BRAVE
    override val packageName = "com.brave.browser"
    override val title = R.string.brave__title
    override val description = R.string.brave__description
    override val installationWarning = R.string.brave__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_brave
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"
    override val projectPage = "https://github.com/brave/brave-browser"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        cacheBehaviour: CacheBehaviour,
    ): LatestUpdate {
        val time = System.nanoTime()
        Log.d(LOG_TAG, "check for latest version")
        val fileName = findNameOfApkFile()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.REPOSITORY__BRAVE__BRAVE_BROWSER,
            resultsPerApiCall = GithubConsumer.RESULTS_PER_API_CALL__BRAVE_BROWSER,
            dontUseApiForLatestRelease = true,
            isValidRelease = { !it.isPreRelease && it.name.startsWith("Release v") },
            isSuitableAsset = { it.name == fileName },
            cacheBehaviour = cacheBehaviour,
        )
        val version = result.tagName.replace("v", "")
        Log.i(LOG_TAG, "found latest version $version after ${(System.nanoTime() - time) / 1000000}ms")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findNameOfApkFile(): String {
        val strings = if (DeviceSdkTester.supportsAndroidNougat()) {
            DeviceAbiExtractor.StringsForAbi(
                armeabi_v7a = "BraveMonoarm.apk",
                arm64_v8a = "BraveMonoarm64.apk",
                x86 = "BraveMonox86.apk",
                x86_64 = "BraveMonox64.apk"
            )
        } else {
            DeviceAbiExtractor.StringsForAbi(
                armeabi_v7a = "Bravearm.apk",
                x86 = "Bravex86.apk",
            )
        }
        return DeviceAbiExtractor.findStringForBestAbi(strings, DeviceSettingsHelper.prefer32BitApks)
    }

    companion object {
        private const val LOG_TAG = "Brave"
    }
}