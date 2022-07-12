package de.marmaro.krt.ffupdater.app.maintained

import android.net.Uri
import android.os.Build
import android.util.Log
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/mozilla-mobile/focus-android
 * https://api.github.com/repos/mozilla-mobile/focus-android/releases
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class FirefoxFocus(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.mozilla.focus"
    override val displayTitle = R.string.firefox_focus__title
    override val displayDescription = R.string.firefox_focus__description
    override val displayDownloadSource = R.string.github
    override val displayIcon = R.mipmap.ic_logo_firefox_focus_klar
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM_AND_X_ABIS
    override val projectPage: Uri = Uri.parse("https://github.com/mozilla-mobile/focus-android")

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"

    override suspend fun findLatestUpdate(): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val fileSuffix = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "armeabi-v7a.apk"
            ABI.ARM64_V8A -> "arm64-v8a.apk"
            ABI.X86 -> "x86.apk"
            ABI.X86_64 -> "x86_64.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val githubConsumer = GithubConsumer(
            repoOwner = "mozilla-mobile",
            repoName = "focus-android",
            resultsPerPage = 3,
            isValidRelease = { release -> !release.isPreRelease && "beta" !in release.name },
            isCorrectAsset = { asset -> asset.name.startsWith("focus") && asset.name.endsWith(fileSuffix) },
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

        val version = extractVersion()
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }

    companion object {
        private const val LOG_TAG = "FirefoxFocus"
    }
}