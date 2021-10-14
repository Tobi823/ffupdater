package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/mozilla-mobile/focus-android
 * https://api.github.com/repos/mozilla-mobile/focus-android/releases
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class FirefoxFocus : BaseAppDetail() {
    override val packageName = "org.mozilla.focus"
    override val displayTitle = R.string.firefox_focus__title
    override val displayDescription = R.string.firefox_focus__description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_firefox_focus_klar
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val fileName = getStringForCurrentAbi("Focus-arm.apk", "Focus-arm64.apk")
        val githubConsumer = GithubConsumer(
            repoOwner = "mozilla-mobile",
            repoName = "focus-android",
            resultsPerPage = 3,
            validReleaseTester = { release: GithubConsumer.Release ->
                !release.isPreRelease &&
                        !release.name.contains("beta") &&
                        release.assets.any { it.name == fileName }
            },
            correctAssetTester = { asset: GithubConsumer.Asset -> asset.name == fileName })
        val result = githubConsumer.updateCheck()
        val versionRegexResult = Regex("""^v((\d)+(\.\d+)*)""").find(result.tagName)
        val version = versionRegexResult!!.groups[1]!!.value
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }
}