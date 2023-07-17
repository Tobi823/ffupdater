package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.FROM_MOZILLA
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/mozilla-mobile/focus-android
 * https://api.github.com/repos/mozilla-mobile/focus-android/releases
 * https://www.apkmirror.com/apk/mozilla/firefox-beta
 */
@Keep
object FirefoxBeta : AppBase() {
    override val app = App.FIREFOX_BETA
    override val packageName = "org.mozilla.firefox_beta"
    override val title = R.string.firefox_beta__title
    override val description = R.string.firefox_beta__description
    override val installationWarning = R.string.firefox_beta__warning
    override val downloadSource = "Github"
    override val icon = R.drawable.ic_logo_firefox_beta
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"
    override val projectPage = "https://github.com/mozilla-mobile/firefox-android"
    override val displayCategory = listOf(FROM_MOZILLA)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val fileSuffix = findFileSuffix()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.REPOSITORY__MOZILLA_MOBILE__FIREFOX_ANDROID,
            resultsPerApiCall = GithubConsumer.RESULTS_PER_API_CALL__FIREFOX_ANDROID,
            dontUseApiForLatestRelease = true,
            isValidRelease = { it.isPreRelease && """^Firefox Beta \d""".toRegex().containsMatchIn(it.name) },
            isSuitableAsset = { it.nameStartsAndEndsWith("fenix-", "-$fileSuffix") },
            cacheBehaviour = cacheBehaviour,
        )
        val version = result.tagName
            .removePrefix("fenix-v") //convert fenix-v112.0b5 to 112.0b5
            .removePrefix("v") //fallback if the tag naming schema changed
        return LatestVersion(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileSuffix(): String {
        val fileSuffix = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a.apk"
            ABI.ARM64_V8A -> "arm64-v8a.apk"
            ABI.X86 -> "x86.apk"
            ABI.X86_64 -> "x86_64.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return fileSuffix
    }
}