package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseApp
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.device.ABI

/**
 * https://github.com/mozilla-mobile/focus-android
 * https://api.github.com/repos/mozilla-mobile/focus-android/releases
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class FirefoxFocus(
    private val failIfValidReleaseHasNoValidAsset: Boolean = false,
    private val apiConsumer: ApiConsumer,
    private val deviceAbis: List<ABI>,
) : BaseApp() {
    override val packageName = "org.mozilla.focus"
    override val displayTitle = R.string.firefox_focus__title
    override val displayDescription = R.string.firefox_focus__description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_firefox_focus_klar
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)
    override val normalInstallation = true

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"

    override suspend fun checkForUpdate(): AvailableVersionResult {
        val filteredAbis = deviceAbis.filter { it in supportedAbis }
        val fileSuffix = when (filteredAbis.firstOrNull()) {
            ABI.ARMEABI_V7A -> "armeabi-v7a.apk"
            ABI.ARM64_V8A -> "arm64-v8a.apk"
            ABI.X86 -> "x86.apk"
            ABI.X86_64 -> "x86_64.apk"
            else -> throw IllegalArgumentException("ABI '${filteredAbis.firstOrNull()}' is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "mozilla-mobile",
            repoName = "focus-android",
            resultsPerPage = 3,
            isValidRelease = { release -> !release.isPreRelease && "beta" !in release.name },
            isCorrectAsset = { asset -> asset.name.startsWith("focus") && asset.name.endsWith(fileSuffix) },
            failIfValidReleaseHasNoValidAsset = failIfValidReleaseHasNoValidAsset,
            apiConsumer = apiConsumer,
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