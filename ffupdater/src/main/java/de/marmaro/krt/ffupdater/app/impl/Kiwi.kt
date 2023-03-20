package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/kiwibrowser/src.next
 * https://api.github.com/repos/kiwibrowser/src.next/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
@Deprecated("app is too old")
class Kiwi(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.KIWI
    override val packageName = "com.kiwibrowser.browser"
    override val title = R.string.kiwi__title
    override val description = R.string.kiwi__description
    override val installationWarning = R.string.kiwi__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_kiwi
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "829b930e919cd56c9a67617c312e3b425a38894b929e735c3d391d9c51b9e4c0"
    override val projectPage = "https://github.com/kiwibrowser/src.next"
    override val displayCategory = DisplayCategory.EOL
    override val eolReason = R.string.generic__eol_reason__too_old

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val abiString = when (deviceAbiExtractor.findBestAbi(supportedAbis, deviceSettings.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "arm"
            ABI.ARM64_V8A -> "arm64"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val fileRegex = Regex.escape("com.kiwibrowser.browser-$abiString-") +
                """\d+""" +
                Regex.escape("-github.apk")

        val result = consumer.updateCheck(
            repoOwner = "kiwibrowser",
            repoName = "src.next",
            initResultsPerPage = 3,
            isValidRelease = { true },
            isSuitableAsset = { Regex(fileRegex).matches(it.name) },
            dontUseApiForLatestRelease = true,
            fileDownloader = fileDownloader,
        )
        // tag name can be "2232087292" (the id of the build runner)
        val version = result.tagName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    override fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate,
    ): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val runnerId = preferences.getString(BUILD_RUNNER_ID, null)
        val fileSize = preferences.getLong(APK_FILE_SIZE, -1)
        return runnerId != available.version || fileSize != available.exactFileSizeBytesOfDownload
    }

    @SuppressLint("ApplySharedPref")
    override fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(BUILD_RUNNER_ID, available.latestUpdate.version)
            .putLong(APK_FILE_SIZE, available.latestUpdate.exactFileSizeBytesOfDownload!!)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appIsInstalledCallback(context, available)
    }

    companion object {
        private const val LOG_TAG = "Kiwi"
        const val BUILD_RUNNER_ID = "kiwi__build_runner_id"
        const val APK_FILE_SIZE = "kiwi__file_size"
    }
}
