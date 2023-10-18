package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BETTER_THAN_GOOGLE_CHROME
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.EOL
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_PRIVACY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://github.com/Alex313031/Thorium-Android
 * https://api.github.com/repos/Alex313031/Thorium-Android/releases
 */
@Keep
object Thorium : AppBase() {
    override val app = App.THORIUM
    override val packageName = "org.chromium.thorium"
    override val title = R.string.thorium__title
    override val description = R.string.thorium__description
    override val installationWarning = R.string.thorium__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_thorium
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = ARM32_ARM64
    override val signatureHash = "32a2fc74d731105859e5a85df16d95f102d85b22099b8064c5d8915c61dad1e0"
    override val projectPage = "https://github.com/Alex313031/Thorium-Android"
    override val displayCategory = listOf(BETTER_THAN_GOOGLE_CHROME)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val fileName = findFileName()
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("Alex313031", "Thorium-Android"),
            resultsPerApiCall = 3,
            dontUseApiForLatestRelease = false,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name == fileName },
            cacheBehaviour = cacheBehaviour,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName.removePrefix("M"),
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findFileName(): String {
        val fileName = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "Thorium_Public_arm32.apk"
            ABI.ARM64_V8A -> "Thorium_Public_arm64.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return fileName
    }
}