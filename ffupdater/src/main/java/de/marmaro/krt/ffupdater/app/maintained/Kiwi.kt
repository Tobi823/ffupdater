package de.marmaro.krt.ffupdater.app.maintained

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import java.io.File

/**
 * https://github.com/kiwibrowser/src.next
 * https://api.github.com/repos/kiwibrowser/src.next/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
class Kiwi(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "com.kiwibrowser.browser"
    override val title = R.string.kiwi__title
    override val displayDescription = R.string.kiwi__description
    override val displayWarning = R.string.kiwi__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_kiwi
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM_AND_X_ABIS
    override val projectPage: Uri = Uri.parse("https://github.com/kiwibrowser/src.next")

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "829b930e919cd56c9a67617c312e3b425a38894b929e735c3d391d9c51b9e4c0"

    override suspend fun findLatestUpdate(): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileSuffix = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "-arm-github.apk"
            ABI.ARM64_V8A -> "-arm64-github.apk"
            ABI.X86 -> "-x86-github.apk"
            ABI.X86_64 -> "-x64-github.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val filePrefix = "com.kiwibrowser.browser-"
        val githubConsumer = GithubConsumer(
            repoOwner = "kiwibrowser",
            repoName = "src.next",
            resultsPerPage = 1,
            isValidRelease = { true },
            isCorrectAsset = { asset -> asset.name.startsWith(filePrefix) && asset.name.endsWith(fileSuffix) },
            dontUseApiForLatestRelease = true,
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()
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