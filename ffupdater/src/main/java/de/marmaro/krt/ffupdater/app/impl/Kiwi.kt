package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/kiwibrowser/src.next
 * https://api.github.com/repos/kiwibrowser/src.next/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
@Keep
object Kiwi : AppBase() {
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
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val abiString = findAbiString()
        val fileRegex = Regex.escape("com.kiwibrowser.browser-$abiString-") +
                """\d+""" +
                Regex.escape("-github.apk")

        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("kiwibrowser", "src.next"),
            resultsPerApiCall = 3,
            isValidRelease = { true },
            isSuitableAsset = { Regex(fileRegex).matches(it.name) },
            dontUseApiForLatestRelease = true,
            cacheBehaviour = cacheBehaviour,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findAbiString(): String {
        val abiString = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "arm"
            ABI.ARM64_V8A -> "arm64"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return abiString
    }

    override fun isInstalledAppOutdated(
        context: Context,
        available: LatestVersion,
    ): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val runnerId = preferences.getString(BUILD_RUNNER_ID, null)
        val fileSize = preferences.getLong(APK_FILE_SIZE, -1)
        return runnerId != available.version || fileSize != available.exactFileSizeBytesOfDownload
    }

    @SuppressLint("ApplySharedPref")
    override suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(BUILD_RUNNER_ID, available.latestVersion.version)
            .putLong(APK_FILE_SIZE, available.latestVersion.exactFileSizeBytesOfDownload!!)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appWasInstalledCallback(context, available)
    }

    private const val BUILD_RUNNER_ID = "kiwi__build_runner_id"
    private const val APK_FILE_SIZE = "kiwi__file_size"
}
