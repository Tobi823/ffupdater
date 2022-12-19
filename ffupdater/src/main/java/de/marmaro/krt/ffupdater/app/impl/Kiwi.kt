package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import java.io.File

/**
 * https://github.com/kiwibrowser/src.next
 * https://api.github.com/repos/kiwibrowser/src.next/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
class Kiwi(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val codeName = "Kiwi"
    override val packageName = "com.kiwibrowser.browser"
    override val title = R.string.kiwi__title
    override val description = R.string.kiwi__description
    override val installationWarning = R.string.kiwi__warning
    override val downloadSource = "GitHub"
    override val icon = R.mipmap.ic_logo_kiwi
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "829b930e919cd56c9a67617c312e3b425a38894b929e735c3d391d9c51b9e4c0"
    override val projectPage = "https://github.com/kiwibrowser/src.next"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val fileRegex = when (deviceAbiExtractor.findBestAbiForDeviceAndApp(
            supportedAbis,
            deviceSettings.prefer32BitApks
        )) {
            ABI.ARMEABI_V7A -> """com\.kiwibrowser\.browser-arm-\d+-github\.apk"""
            ABI.ARM64_V8A -> """com\.kiwibrowser\.browser-arm64-\d+-github\.apk"""
            ABI.X86 -> """com\.kiwibrowser\.browser-x86-\d+-github\.apk"""
            ABI.X86_64 -> """com\.kiwibrowser\.browser-x64-\d+-github\.apk"""
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = consumer.updateCheck(
            repoOwner = "kiwibrowser",
            repoName = "src.next",
            resultsPerPage = 3,
            isValidRelease = { release ->
                release.assets.any { asset -> asset.name.endsWith(".apk") }
            },
            isSuitableAsset = { asset ->
                Regex(fileRegex).matches(asset.name)
            },
            dontUseApiForLatestRelease = true,
            settings = networkSettings
        )
        // tag name can be "2232087292" (the id of the build runner)
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

    override suspend fun isAvailableVersionEqualToArchive(
        context: Context,
        file: File,
        available: LatestUpdate,
    ): Boolean {
        // identify file (and the correctness of the cache) by its size
        // (not perfect but the last 30 releases every file has a different size)
        // chance of 100 releases that two released files has the same size is 0,031%
        // (assuming that 10^8 file sizes exists)
        return file.length() == available.fileSizeBytes
    }

    override fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate,
    ): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val runnerId = preferences.getString(BUILD_RUNNER_ID, null)
        val fileSize = preferences.getLong(APK_FILE_SIZE, -1)
        return runnerId != available.version || fileSize != available.fileSizeBytes
    }

    override fun appIsInstalled(context: Context, available: AppUpdateStatus) {
        super.appIsInstalled(context, available)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(BUILD_RUNNER_ID, available.version)
            .putLong(APK_FILE_SIZE, available.fileSizeBytes!!)
            .apply()
    }

    companion object {
        private const val LOG_TAG = "Kiwi"
        const val BUILD_RUNNER_ID = "kiwi__build_runner_id"
        const val APK_FILE_SIZE = "kiwi__file_size"
    }
}
