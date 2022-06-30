package de.marmaro.krt.ffupdater.app.maintained

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableAppVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import java.io.File

/**
 * https://github.com/kiwibrowser/src.next
 * https://api.github.com/repos/kiwibrowser/src.next/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
class Kiwi(
    private val failIfValidReleaseHasNoValidAsset: Boolean = false,
    private val apiConsumer: ApiConsumer,
    private val deviceAbis: List<ABI>,
) : AppBase() {
    override val packageName = "com.kiwibrowser.browser"
    override val displayTitle = R.string.kiwi__title
    override val displayDescription = R.string.kiwi__description
    override val displayWarning = R.string.kiwi__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_kiwi
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86, ABI.X86_64)
    override val normalInstallation = true

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "829b930e919cd56c9a67617c312e3b425a38894b929e735c3d391d9c51b9e4c0"

    override suspend fun checkForUpdate(): AvailableAppVersion {
        val filteredAbis = deviceAbis.filter { it in supportedAbis }
        val filePrefix = "com.kiwibrowser.browser-"
        val fileSuffix = when (filteredAbis.firstOrNull()) {
            ABI.ARMEABI_V7A -> "-arm-github.apk"
            ABI.ARM64_V8A -> "-arm64-github.apk"
            ABI.X86 -> "-x86-github.apk"
            ABI.X86_64 -> "-x64-github.apk"
            else -> throw IllegalArgumentException("ABI '${filteredAbis.firstOrNull()}' is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "kiwibrowser",
            repoName = "src.next",
            resultsPerPage = 1,
            isValidRelease = { true },
            failIfValidReleaseHasNoValidAsset = failIfValidReleaseHasNoValidAsset,
            isCorrectAsset = { asset -> asset.name.startsWith(filePrefix) && asset.name.endsWith(fileSuffix) },
            dontUseApiForLatestRelease = true,
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()
        // tag name can be "2232087292" (the id of the build runner)
        return AvailableAppVersion(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }

    override suspend fun isAvailableVersionEqualToArchive(
        context: Context,
        file: File,
        available: AvailableAppVersion,
    ): Boolean {
        // identify file (and the correctness of the cache) by its size
        // (not perfect but the last 30 releases every file has a different size)
        // chance of 100 releases that two released files has the same size is 0,031%
        // (assuming that 10^8 file sizes exists)
        return file.length() == available.fileSizeBytes
    }

    override fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: AvailableAppVersion,
    ): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val runnerId = preferences.getString(BUILD_RUNNER_ID, null)
        val fileSize = preferences.getLong(APK_FILE_SIZE, -1)
        return runnerId != available.version || fileSize != available.fileSizeBytes
    }

    override fun appIsInstalled(context: Context, available: AvailableAppVersion) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(BUILD_RUNNER_ID, available.version)
            .putLong(APK_FILE_SIZE, available.fileSizeBytes!!)
            .apply()
    }

    companion object {
        const val BUILD_RUNNER_ID = "kiwi__build_runner_id"
        const val APK_FILE_SIZE = "kiwi__file_size"
    }
}