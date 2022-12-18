package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://github.com/mozilla-mobile/focus-android
 * https://api.github.com/repos/mozilla-mobile/focus-android/releases
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class FirefoxFocus(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val codeName = "FirefoxFocus"
    override val packageName = "org.mozilla.focus"
    override val title = R.string.firefox_focus__title
    override val description = R.string.firefox_focus__description
    override val downloadSource = "GitHub"
    override val icon = R.mipmap.ic_logo_firefox_focus_klar
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64_X86_X64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"
    override val projectPage = "https://github.com/mozilla-mobile/focus-android"
    override val displayCategory = DisplayCategory.FROM_MOZILLA

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val settings = NetworkSettingsHelper(context)
        val fileSuffix = when (deviceAbiExtractor.findBestAbiForDeviceAndApp(supportedAbis)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a.apk"
            ABI.ARM64_V8A -> "arm64-v8a.apk"
            ABI.X86 -> "x86.apk"
            ABI.X86_64 -> "x86_64.apk"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = consumer.updateCheck(
            repoOwner = "mozilla-mobile",
            repoName = "focus-android",
            resultsPerPage = 3,
            isValidRelease = { release ->
                !release.isPreRelease &&
                        "beta" !in release.name &&
                        release.assets.any { asset -> asset.name.endsWith(".apk") }
            },
            isSuitableAsset = { asset ->
                asset.name.startsWith("focus") &&
                        asset.name.endsWith(fileSuffix)
            },
            dontUseApiForLatestRelease = false,
            settings = settings
        )

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