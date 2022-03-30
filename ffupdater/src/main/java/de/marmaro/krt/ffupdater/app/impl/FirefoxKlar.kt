package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppWithCachedUpdateCheck
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/mozilla-mobile/focus-android
 * https://api.github.com/repos/mozilla-mobile/focus-android/releases
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-klar-the-privacy-browser-2/
 */
class FirefoxKlar(
    private val failIfValidReleaseHasNoValidAsset: Boolean = false
) : BaseAppWithCachedUpdateCheck() {
    override val packageName = "org.mozilla.klar"
    override val displayTitle = R.string.firefox_klar__title
    override val displayDescription = R.string.firefox_klar__description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_firefox_focus_klar
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val fileSuffix = getStringForCurrentAbi("armeabi-v7a.apk", "arm64-v8a.apk", "x86.apk", "x86_64.apk")
        val githubConsumer = GithubConsumer(
            repoOwner = "mozilla-mobile",
            repoName = "focus-android",
            resultsPerPage = 3,
            isValidRelease = { release -> !release.isPreRelease && !release.name.contains("beta") },
            isCorrectAsset = { asset -> asset.name.startsWith("klar") && asset.name.endsWith(fileSuffix) },
            failIfValidReleaseHasNoValidAsset = failIfValidReleaseHasNoValidAsset,
        )
        val result = githubConsumer.updateCheck()

        val extractVersion = {
            val regexMatch = Regex("""^v((\d)+(\.\d+)*)""")
                .find(result.tagName)
            checkNotNull(regexMatch) {
                "Fail to extract the version with regex from string: \"${result.tagName}\""
            }
            val matchGroup = regexMatch.groups[1]
            checkNotNull(matchGroup) {
                "Fail to extract the version value from regex match: \"${regexMatch.value}\""
            }
            matchGroup.value
        }

        return AvailableVersionResult(
            downloadUrl = result.url,
            version = extractVersion(),
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null
        )
    }
}