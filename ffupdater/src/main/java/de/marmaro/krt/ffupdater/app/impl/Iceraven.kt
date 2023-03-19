package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/fork-maintainers/iceraven-browser
 * https://api.github.com/repos/fork-maintainers/iceraven-browser/releases
 */
class Iceraven(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.ICERAVEN
    override val codeName = "Iceraven"
    override val packageName = "io.github.forkmaintainers.iceraven"
    override val title = R.string.iceraven__title
    override val description = R.string.iceraven__description
    override val installationWarning = R.string.iceraven__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_iceraven
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c0d22379f487b70a4f9f8bec0173cf91a1644f08f93385b5b782ce37660ba81"
    override val projectPage = "https://github.com/fork-maintainers/iceraven-browser"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    override fun getInstalledVersion(context: Context): String? {
        val installedVersion = super.getInstalledVersion(context)
        return installedVersion?.replace("iceraven-", "")
    }

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate? {
        Log.d(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val fileSuffix = when (deviceAbiExtractor.findBestAbi(
            supportedAbis,
            deviceSettings.prefer32BitApks
        )) {
            ABI.ARMEABI_V7A -> "browser-armeabi-v7a-forkRelease.apk"
            ABI.ARM64_V8A -> "browser-arm64-v8a-forkRelease.apk"
            ABI.X86 -> "browser-x86-forkRelease.apk"
            ABI.X86_64 -> "browser-x86_64-forkRelease.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = consumer.updateCheck(
            repoOwner = "fork-maintainers",
            repoName = "iceraven-browser",
            initResultsPerPage = 3,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(fileSuffix) },
            dontUseApiForLatestRelease = false,
            fileDownloader = fileDownloader,
        )
        val version = result.tagName.replace("iceraven-", "")
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    companion object {
        private const val LOG_TAG = "Iceraven"
    }
}