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
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://github.com/brave/brave-browser/releases
 * https://api.github.com/repos/brave/brave-browser/releases
 * https://www.apkmirror.com/apk/brave-software/brave-browser-beta/
 */
class BraveBeta(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) : AppBase() {
    override val app = App.BRAVE_BETA
    override val codeName = "BraveBeta"
    override val packageName = "com.brave.browser_beta"
    override val title = R.string.brave_beta__title
    override val description = R.string.brave_beta__description
    override val installationWarning = R.string.brave__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_brave_beta
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"
    override val projectPage = "https://github.com/brave/brave-browser"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate? {
        Log.d(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val fileName = getNameOfApkFile(deviceSettings)
        val result = consumer.updateCheckFor_Brave_BraveBrowser(
            isValidRelease = { !it.isPreRelease && it.name.startsWith("Beta v") },
            isSuitableAsset = { it.name == fileName },
            fileDownloader = fileDownloader,
        )
        val version = result.tagName.replace("v", "")
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun getNameOfApkFile(settings: DeviceSettingsHelper): String {
        val fileName = if (deviceSdkTester.supportsAndroidNougat()) {
            when (deviceAbiExtractor.findBestAbi(supportedAbis, settings.prefer32BitApks)) {
                ABI.ARMEABI_V7A -> "BraveMonoarm.apk"
                ABI.ARM64_V8A -> "BraveMonoarm64.apk"
                ABI.X86 -> "BraveMonox86.apk"
                ABI.X86_64 -> "BraveMonox64.apk"
                else -> throw IllegalArgumentException("ABI for Android 7+ is not supported")
            }
        } else {
            when (deviceAbiExtractor.findBestAbi(ARM32_X86, settings.prefer32BitApks)) {
                ABI.ARMEABI_V7A -> "Bravearm.apk"
                ABI.X86 -> "Bravex86.apk"
                else -> throw IllegalArgumentException("ABI for Android 6 is not supported")
            }
        }
        return fileName
    }

    companion object {
        private const val LOG_TAG = "BraveBeta"
    }
}