package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BASED_ON_FIREFOX
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_PRIVACY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.gitlab.GitLabConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import java.util.function.Predicate

@Keep
object Ironfox : AppBase() {
    override val app = App.IRONFOX
    override val packageName = "org.ironfoxoss.ironfox"
    override val title = R.string.ironfox__title
    override val description = R.string.ironfox__description
    override val installationWarning = R.string.ironfox__warning
    override val downloadSource = "GitLab"
    override val icon = R.drawable.ic_logo_ironfox
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = listOf(ABI.ARMEABI_V7A, ABI.ARM64_V8A, ABI.X86, ABI.X86_64)
    override val signatureHash = "c5e291b5a571f9c8cd9a9799c2c94e02ec9703948893f2ca756d67b94204f904"
    override val projectPage = "https://gitlab.com/ironfox-oss/IronFox"
    override val displayCategory = listOf(BASED_ON_FIREFOX, GOOD_PRIVACY_BROWSER)
    private val REPOSITORY = GitLabConsumer.GitLabRepo(
        owner = "ironfox-oss",
        name = "IronFox",
        projectId = 65779408,
        irrelevantReleasesBetweenRelevant = 0
    )
    override val hostnameForInternetCheck = "https://gitlab.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        // determine ABI suffix for asset matching
        val abiSuffix = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A   -> "arm64-v8a"
            ABI.X86         -> "x86"
            ABI.X86_64      -> "x86_64"
            else            -> throw IllegalArgumentException("Unsupported ABI")
        }

        val result = GitLabConsumer.findLatestRelease(
            repository = REPOSITORY,
            isValidRelease = Predicate { true },
            isSuitableAsset = Predicate { asset -> asset.name.endsWith("-$abiSuffix.apk") },
            requireReleaseDescription = false
        )

        val cleanedVersion = result.tagName.removePrefix("v")
        return LatestVersion(
            downloadUrl = result.url,
            version = Version(cleanedVersion),
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = null,
            fileHash = null
        )
    }
}
