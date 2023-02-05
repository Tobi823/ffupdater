package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://github.com/guardianproject/orbot
 * https://api.github.com/repos/guardianproject/orbot/releases
 * https://www.apkmirror.com/apk/the-tor-project/orbot-proxy-with-tor/
 */
class Orbot(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.ORBOT
    override val codeName = "Orbot"
    override val packageName = "org.torproject.android"
    override val title = R.string.orbot__title
    override val description = R.string.orbot__description
    override val installationWarning: Int? = null
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_orbot
    override val minApiLevel = Build.VERSION_CODES.JELLY_BEAN
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "a454b87a1847a89ed7f5e70fba6bba96f3ef29c26e0981204fe347bf231dfd5b"
    override val projectPage = "https://github.com/guardianproject/orbot"
    override val displayCategory = DisplayCategory.OTHER

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val assetSuffix = getAssetSuffix(deviceSettings)
        val result = consumer.updateCheck(
            repoOwner = "guardianproject",
            repoName = "orbot",
            initResultsPerPage = 3,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.nameStartsOrEnds("Orbot", assetSuffix) },
            dontUseApiForLatestRelease = false,
            settings = networkSettings
        )
        val version = result.tagName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun getAssetSuffix(settings: DeviceSettingsHelper): String {
        return when (deviceAbiExtractor.findBestAbi(supportedAbis, settings.prefer32BitApks)) {
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
