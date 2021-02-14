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
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases
 */
class Lockwise(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "mozilla.lockbox"
    override val displayTitle = R.string.lockwise_title
    override val displayDescription = R.string.lockwise_description
    override val displayWarning = R.string.lockwise_warning
    override val displayDownloadSource = R.string.github
    override val signatureHash = "64d26b507078deba2fee42d6bd0bfad41d39ffc4e791f281028e5e73d3c8d2f2"
    override val minApiLevel = Build.VERSION_CODES.N
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
                repoOwner = "mozilla-lockwise",
                repoName = "lockwise-android",
                resultsPerPage = 5,
                validReleaseTester = { release: Release ->
                    !release.isPreRelease && release.assets.any { it.name.endsWith(".apk") }
                },
                correctDownloadUrlTester = { asset: Asset -> asset.name.endsWith(".apk") })
        val result = githubConsumer.updateCheck()
        // tag_name can be: "release-v4.0.3", "release-v4.0.0-RC-2"
        val regexResult = Regex("""^release-v((\d)+(\.\d+)*)""").find(result.tagName)
        val version = regexResult?.groups?.get(1)?.value
                ?: throw RuntimeException("regex pattern does not match '${result.tagName}'")
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