package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/brave/brave-browser/releases
 * https://api.github.com/repos/brave/brave-browser/releases
 * https://www.apkmirror.com/apk/brave-software/brave-browser-nightly
 */
class BraveNightly(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "com.brave.browser_nightly"
    override val title = R.string.brave_nightly__title
    override val description = R.string.brave_nightly__description
    override val installationWarning = R.string.brave__warning
    override val downloadSource = R.string.github
    override val icon = R.mipmap.ic_logo_brave_nightly
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"
    override val projectPage = "https://github.com/brave/brave-browser"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileName = getNameOfApkFile()
        val result = try {
            consumer.updateCheck(
                repoOwner = "brave",
                repoName = "brave-browser",
                resultsPerPage = 10,
                isValidRelease = { release -> !release.isPreRelease && release.name.startsWith("Nightly v") },
                isSuitableAsset = { asset -> asset.name == fileName },
                dontUseApiForLatestRelease = true,
                context
            )
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of Brave Nightly.", e)
        }
        val version = result.tagName.replace("v", "")
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }

    private fun getNameOfApkFile(): String {
        return if (DeviceSdkTester.supportsAndroidNougat()) {
            when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
                ABI.ARMEABI_V7A -> "BraveMonoarm.apk"
                ABI.ARM64_V8A -> "BraveMonoarm64.apk"
                ABI.X86 -> "BraveMonox86.apk"
                ABI.X86_64 -> "BraveMonox64.apk"
                else -> throw IllegalArgumentException("ABI for Android 7+ is not supported")
            }
        } else {
            when (deviceAbiExtractor.supportedAbis.first { abi -> abi in ARM32_X86 }) {
                ABI.ARMEABI_V7A -> "Bravearm.apk"
                ABI.X86 -> "Bravex86.apk"
                else -> throw IllegalArgumentException("ABI for Android 6 is not supported")
            }
        }
    }

    companion object {
        private const val LOG_TAG = "BraveNightly"
    }
}