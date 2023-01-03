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
 * https://github.com/ungoogled-software/ungoogled-chromium-android/releases
 */
@Deprecated("app is no longer supported")
class UngoogledChromium(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.UNGOOGLED_CHROMIUM
    override val codeName = "UngoogledChromium"
    override val packageName = "org.ungoogled.chromium.stable"
    override val title = R.string.ungoogled_chromium__title
    override val description = R.string.ungoogled_chromium__description
    override val installationWarning = R.string.ungoogled_chromium__warning
    override val downloadSource = "GitHub"
    override val icon = R.mipmap.ic_logo_ungoogled_chromium
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86)
    override val projectPage = "https://github.com/ungoogled-software/ungoogled-chromium-android"

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "7e6ba7bbb939fa52d5569a8ea628056adf8c75292bf4dee6b353fafaf2c30e19"
    override val eolReason = R.string.ungoogled_chromium__eol_reason
    override val displayCategory = DisplayCategory.EOL

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val fileName = when (deviceAbiExtractor.findBestAbi(
            supportedAbis,
            deviceSettings.prefer32BitApks
        )) {
            ABI.ARMEABI_V7A -> "ChromeModernPublic_arm.apk"
            ABI.ARM64_V8A -> "ChromeModernPublic_arm64.apk"
            ABI.X86 -> "ChromeModernPublic_x86.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = consumer.updateCheck(
            repoOwner = "ungoogled-software",
            repoName = "ungoogled-chromium-android",
            initResultsPerPage = 2,
            isValidRelease = { !it.isPreRelease && "webview" !in it.name },
            isSuitableAsset = { it.name == fileName },
            dontUseApiForLatestRelease = true,
            settings = networkSettings
        )

        val version = result.tagName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }

    companion object {
        private const val LOG_TAG = "UngooChromium"
    }
}