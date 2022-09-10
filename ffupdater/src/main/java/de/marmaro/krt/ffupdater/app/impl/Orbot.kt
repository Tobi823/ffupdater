package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.Category
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/guardianproject/orbot
 * https://api.github.com/repos/guardianproject/orbot/releases
 * https://www.apkmirror.com/apk/the-tor-project/orbot-proxy-with-tor/
 */
class Orbot(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.torproject.android"
    override val title = R.string.orbot__title
    override val description = R.string.orbot__description
    override val installationWarning: Int? = null
    override val downloadSource = R.string.github
    override val icon = R.mipmap.ic_logo_orbot
    override val minApiLevel = Build.VERSION_CODES.JELLY_BEAN
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "a454b87a1847a89ed7f5e70fba6bba96f3ef29c26e0981204fe347bf231dfd5b"
    override val projectPage = "https://github.com/guardianproject/orbot"
    override val displayCategory = Category.OTHER

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val result = try {
            val assetSuffix = getAssetSuffix()
            consumer.updateCheck(
                repoOwner = "guardianproject",
                repoName = "orbot",
                resultsPerPage = 3,
                isValidRelease = { release -> !release.isPreRelease },
                isSuitableAsset = { asset ->
                    asset.name.startsWith("Orbot") && asset.name.endsWith(assetSuffix)
                },
                dontUseApiForLatestRelease = false,
                context
            )
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of Orbot.", e)
        }
        Log.i(LOG_TAG, "found latest version ${result.tagName}")
        return LatestUpdate(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
            fileHash = null,
        )
    }

    private fun getAssetSuffix(): String {
        return when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "-fullperm-universal-release.apk"
            ABI.ARM64_V8A -> "-fullperm-arm64-v8a-release.apk"
            ABI.X86 -> "-fullperm-universal-release.apk"
            ABI.X86_64 -> "-fullperm-universal-release.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }

    companion object {
        private const val LOG_TAG = "Orbot"
    }
}
