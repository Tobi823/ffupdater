package de.marmaro.krt.ffupdater.app.impl

import android.graphics.Color
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Asset
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Release
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import java.time.format.DateTimeFormatter

/**
 * https://github.com/kiwibrowser/src
 * https://api.github.com/repos/kiwibrowser/src/releases
 * https://www.apkmirror.com/apk/geometry-ou/kiwi-browser-fast-quiet/
 */
class Kiwi(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "com.kiwibrowser.browser"
    override val displayTitle = R.string.kiwi__title
    override val displayDescription = R.string.kiwi__description
    override val displayWarning = R.string.kiwi__warning
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_kiwi
    override val displayIconBackground = Color.parseColor("#FFFFFF")
    override val minApiLevel = Build.VERSION_CODES.JELLY_BEAN
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)
    @Suppress("SpellCheckingInspection")
    override val signatureHash = "829b930e919cd56c9a67617c312e3b425a38894b929e735c3d391d9c51b9e4c0"

    override suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment): AvailableVersionResult {
        val fileNameRegex = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> Regex("""Kiwi-(\d+)-arm64-signed\.apk""")
                ABI.ARMEABI_V7A -> Regex("""Kiwi-(\d+)-arm-signed\.apk""")
                ABI.X86 -> Regex("""Kiwi-(\d+)-x86-signed\.apk""")
                ABI.X86_64 -> Regex("""Kiwi-(\d+)-x64-signed\.apk""")
                ABI.ARMEABI, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
        val githubConsumer = GithubConsumer(
                apiConsumer = apiConsumer,
                repoOwner = "kiwibrowser",
                repoName = "src",
                resultsPerPage = 1,
                validReleaseTester = { release: Release ->
                    release.assets.any { fileNameRegex.matches(it.name) }
                },
                correctDownloadUrlTester = { asset: Asset -> fileNameRegex.matches(asset.name) })
        val result = githubConsumer.updateCheckReliableForReleasesAndPreReleases()
        // tag_name can be "v1.23.68"
        val date = DateTimeFormatter.ofPattern("yyMMdd").format(result.releaseDate)
        val number = result.tagName.replace("v", "")
        val version = "Git${date}Gen${number}"
        return AvailableVersionResult(
                downloadUrl = result.url,
                version = version,
                publishDate = result.releaseDate,
                fileSizeBytes = result.fileSizeBytes)
    }
}