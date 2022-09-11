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
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/bromite/bromite/releases
 * https://api.github.com/repos/bromite/bromite/releases
 * https://www.apkmirror.com/apk/bromite/bromite/
 */
class Bromite(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.bromite.bromite"
    override val title = R.string.bromite__title
    override val description = R.string.bromite__description
    override val installationWarning = R.string.bromite__warning
    override val downloadSource = R.string.github
    override val icon = R.mipmap.ic_logo_bromite
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e1ee5cd076d7b0dc84cb2b45fb78b86df2eb39a3b6c56ba3dc292a5e0c3b9504"
    override val projectPage = "https://github.com/bromite/bromite"
    override val displayCategory = DisplayCategory.GOOD_PRIVACY_BROWSER

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileName = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "arm_ChromePublic.apk"
            ABI.ARM64_V8A -> "arm64_ChromePublic.apk"
            ABI.X86 -> "x86_ChromePublic.apk"
            ABI.X86_64 -> "x64_ChromePublic.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = try {
            consumer.updateCheck(
                repoOwner = "bromite",
                repoName = "bromite",
                resultsPerPage = 5,
                isValidRelease = { release -> !release.isPreRelease },
                isSuitableAsset = { asset -> asset.name == fileName },
                dontUseApiForLatestRelease = false,
                context
            )
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of Bromite.", e)
        }
        // tag name can be "90.0.4430.59"
        Log.i(LOG_TAG, "found latest version ${result.tagName}")
        return LatestUpdate(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }

    companion object {
        private const val LOG_TAG = "Bromite"
    }
}