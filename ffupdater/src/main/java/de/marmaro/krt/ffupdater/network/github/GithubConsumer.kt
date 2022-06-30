package de.marmaro.krt.ffupdater.network.github

import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import java.util.*
import java.util.function.Predicate

class GithubConsumer(
    repoOwner: String,
    repoName: String,
    private val resultsPerPage: Int,
    private val isValidRelease: Predicate<Release>,
    private val isCorrectAsset: Predicate<Asset>,
    // false -> contact "$url/latest" and then "$url?per_page=..&page=.."
    // true -> contact only "$url?per_page=..&page=.."
    // set it to true if it is unlikely that the latest release is a valid release
    private val dontUseApiForLatestRelease: Boolean = false,
    private val apiConsumer: ApiConsumer,
) {
    private val url = "https://api.github.com/repos/$repoOwner/$repoName/releases"

    init {
        check(resultsPerPage > 0)
    }

    @MainThread
    suspend fun updateCheck(): Result {
        val start = if (dontUseApiForLatestRelease) 1 else 0
        var firstReleaseHasAssets = true
        for (tries in start..5) {
            val releases = if (tries == 0) {
                val url = "$url/latest"
                arrayOf(apiConsumer.consumeAsync(url, Release::class).await())
            } else {
                val url = "$url?per_page=$resultsPerPage&page=${tries}"
                apiConsumer.consumeAsync(url, Array<Release>::class).await()
            }

            releases
                .filter { release -> isValidRelease.test(release) }
                .forEach { release ->
                    release.assets
                        .firstOrNull { asset -> isCorrectAsset.test(asset) }
                        ?.let { asset ->
                            return Result(
                                tagName = release.tagName,
                                url = asset.downloadUrl,
                                fileSizeBytes = asset.fileSizeBytes,
                                releaseDate = release.publishedAt,
                                moreRecentReleaseWasIgnoredBecauseItHasNoValidAssets = firstReleaseHasAssets,
                            )
                        }
                    // this will only be called if the first valid release has no valid assets
                    firstReleaseHasAssets = true
                }
        }
        throw InvalidApiResponseException("can't find release after all tries - abort")
    }

    data class Release(
        @SerializedName("tag_name")
        val tagName: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("prerelease")
        val isPreRelease: Boolean,
        @SerializedName("assets")
        val assets: List<Asset>,
        @SerializedName("published_at")
        val publishedAt: String,
    )

    data class Asset(
        @SerializedName("name")
        val name: String,
        @SerializedName("browser_download_url")
        val downloadUrl: String,
        @SerializedName("size")
        val fileSizeBytes: Long,
    )

    data class Result(
        val tagName: String,
        val url: String,
        val fileSizeBytes: Long,
        val releaseDate: String,
        val moreRecentReleaseWasIgnoredBecauseItHasNoValidAssets: Boolean
    )
}