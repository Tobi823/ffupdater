package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.EOL
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/kiwibrowser/src.next
 * https://api.github.com/repos/kiwibrowser/src.next/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
@Keep
@Deprecated("long time no update")
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
    override val signatureHash = "829b930e919cd56c9a67617c312e3b425a38894b929e735c3d391d9c51b9e4c0"
    override val projectPage = "https://github.com/kiwibrowser/src.next"
    override val displayCategory = listOf(EOL)
    override val hostnameForInternetCheck = "https://api.github.com"
    override val eolReason = R.string.eol_reason__browser_no_longer_maintained

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val abiString = findAbiString()
        val fileRegex = Regex.escape("com.kiwibrowser.browser-$abiString-") +
                """\d+""" +
                Regex.escape("-github.apk")

        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("kiwibrowser", "src.next", 0),
            isValidRelease = { true },
            isSuitableAsset = { Regex(fileRegex).matches(it.name) },
            requireReleaseDescription = true,
        )

        val versionPattern = """([\d\.]+)"""
        val versionRegex = Regex("""^\s*Version\s+$versionPattern\s*\n""")
        checkNotNull(result.releaseDescription) { "releaseDescription is null" }
        val match = versionRegex.find(result.releaseDescription)
        checkNotNull(match) { "Did not find a match." }
        val version = match.groups[1]?.value
        checkNotNull(version) { "Did not find version." }

        return LatestVersion(
            downloadUrl = result.url,
            version = Version(version),
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
}
