package de.marmaro.krt.ffupdater.app.maintained

import android.net.Uri
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases
 * https://www.apkmirror.com/apk/mozilla/firefox-lockwise/
 */
class Lockwise(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
) : AppBase() {
    override val packageName = "mozilla.lockbox"
    override val title = R.string.lockwise__title
    override val description = R.string.lockwise__description
    override val downloadSource = R.string.github
    override val icon = R.mipmap.ic_logo_lockwise
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = ALL_ABIS
    override val projectPage: Uri = Uri.parse("https://github.com/mozilla-lockwise/lockwise-android")
    override val eolReason = R.string.lockwise__eol_reason

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "64d26b507078deba2fee42d6bd0bfad41d39ffc4e791f281028e5e73d3c8d2f2"

    override suspend fun findLatestUpdate(): LatestUpdate {
        val githubConsumer = GithubConsumer(
            repoOwner = "mozilla-lockwise",
            repoName = "lockwise-android",
            resultsPerPage = 5,
            isValidRelease = { release -> !release.isPreRelease },
            isCorrectAsset = { asset -> asset.name.endsWith(".apk") },
            apiConsumer = apiConsumer,
        )
        val result = githubConsumer.updateCheck()

        val extractVersion = {
            // tag_name can be: "release-v4.0.3", "release-v4.0.0-RC-2"
            val regexMatch = Regex("""^release-v((\d)+(\.\d+)*)""")
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

        return LatestUpdate(
            downloadUrl = result.url,
            version = extractVersion(),
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
            firstReleaseHasAssets = result.firstReleaseHasAssets,
        )
    }
}