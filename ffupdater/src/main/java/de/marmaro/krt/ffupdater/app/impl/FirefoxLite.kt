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
 * https://api.github.com/repos/mozilla-tw/FirefoxLite/releases
 */
class FirefoxLite(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.mozilla.rocket"
    override val displayTitle = R.string.firefox_lite_title
    override val displayDescription = R.string.firefox_lite_description
    override val displayWarning = R.string.firefox_lite_warning
    override val displayDownloadSource = R.string.github
    override val signatureHash = "863a46f0973932b7d0199b549112741c2d2731ac72ea11b7523aa90a11bf5691"
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.ARMEABI, ABI.X86_64,
            ABI.X86, ABI.MIPS, ABI.MIPS64)

    override fun getDisplayInstalledVersion(context: Context): String {
        return context.getString(R.string.installed_version, getInstalledVersionFromPackageManager(context))
    }

    override fun getInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override fun updateCheckBlocking(context: Context,
                                     deviceEnvironment: DeviceEnvironment): UpdateCheckSubResult {
        val githubConsumer = GithubConsumer(
                apiConsumer = apiConsumer,
                repoOwner = "mozilla-tw",
                repoName = "FirefoxLite",
                resultsPerPage = 5,
                validReleaseTester = { release: Release ->
                    !release.isPreRelease && release.assets.any { it.name.endsWith(".apk") }
                },
                correctDownloadUrlTester = { asset: Asset -> asset.name.endsWith(".apk") }
        )
        val result = githubConsumer.updateCheck()
        // tag_name can be: "v2.5.1", "v.2.0.5"
        val version = result.tagName.replace("v", "")
        return UpdateCheckSubResult(
                downloadUrl = result.url,
                version = version,
                displayVersion = context.getString(R.string.available_version, version),
                publishDate = result.releaseDate,
                fileHashSha256 = null,
                fileSizeBytes = result.fileSizeBytes)
    }

    override fun installationCallback(context: Context, installedVersion: String) {}
}