package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.UpdateCheckSubResult
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Asset
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer.Release
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * https://api.github.com/repos/fork-maintainers/iceraven-browser/releases
 */
class Iceraven(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "io.github.forkmaintainers.iceraven"
    override val displayTitle = R.string.iceraven_title
    override val displayDescription = R.string.iceraven_description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.github
    override val signatureHash = "9c0d22379f487b70a4f9f8bec0173cf91a1644f08f93385b5b782ce37660ba81"
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)

    override fun getDisplayInstalledVersion(context: Context): String {
        val version = getInstalledVersion(context)?.replace("iceraven-", "")
        return context.getString(R.string.installed_version, version ?: "")
    }

    override fun getDisplayAvailableVersion(context: Context, updateCheckSubResult: UpdateCheckSubResult): String {
        val version = updateCheckSubResult.version.replace("iceraven-", "")
        return context.getString(R.string.available_version, version)
    }

    override suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment): UpdateCheckSubResult {
        val fileSuffix = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> "browser-arm64-v8a-forkRelease.apk"
                ABI.ARMEABI_V7A -> "browser-armeabi-v7a-forkRelease.apk"
                ABI.X86 -> "browser-x86-forkRelease.apk"
                ABI.X86_64 -> "browser-x86_64-forkRelease.apk"
                ABI.ARMEABI, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
        val githubConsumer = GithubConsumer(
                apiConsumer = apiConsumer,
                repoOwner = "fork-maintainers",
                repoName = "iceraven-browser",
                resultsPerPage = 3,
                validReleaseTester = { release: Release ->
                    !release.isPreRelease && release.assets.any { it.name.endsWith(".apk") }
                },
                correctDownloadUrlTester = { asset: Asset -> asset.name.endsWith(fileSuffix) })
        val result = githubConsumer.updateCheck()
        val version = result.tagName
        return UpdateCheckSubResult(
                downloadUrl = result.url,
                version = version,
                publishDate = result.releaseDate,
                fileSizeBytes = result.fileSizeBytes)
    }
}