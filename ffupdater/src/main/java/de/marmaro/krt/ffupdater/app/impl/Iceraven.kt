package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
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
    override val supportedAbis = listOf(ABI.AARCH64, ABI.ARM, ABI.X86_64, ABI.X86)

    override fun getDisplayInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override fun getInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override fun updateCheck(context: Context, deviceEnvironment: DeviceEnvironment): UpdateCheckResult {
        check(deviceEnvironment.abis.isNotEmpty())
        val fileSuffix = when (deviceEnvironment.abis[0]) {
            ABI.AARCH64 -> "browser-arm64-v8a-forkRelease.apk"
            ABI.ARM -> "browser-armeabi-v7a-forkRelease.apk"
            ABI.X86 -> "browser-x86-forkRelease.apk"
            ABI.X86_64 -> "browser-x86_64-forkRelease.apk"
        }
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
        val version = result.tagName.replace("iceraven-", "")
        val updateAvailable = getInstalledVersion(context) != version
        return UpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                downloadUrl = result.url,
                version = version,
                displayVersion = version,
                metadata = mapOf(UpdateCheckResult.FILE_SIZE_BYTES to result.fileSizeBytes))
    }

    override fun installationCallback(context: Context, installedVersion: String) {}
}