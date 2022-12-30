package de.marmaro.krt.ffupdater.network.github

import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import java.util.*
import java.util.function.Predicate

class GithubConsumer(private val apiConsumer: ApiConsumer) {

    @MainThread
    @Throws(NetworkException::class)
    suspend fun updateCheck(
        repoOwner: String,
        repoName: String,
        resultsPerPage: Int,
        isValidRelease: Predicate<Release>,
        isSuitableAsset: Predicate<Asset>,
        // false -> contact "$url/latest" and then "$url?per_page=..&page=.."
        // true -> contact only "$url?per_page=..&page=.."
        // set it to true if it is unlikely that the latest release is a valid release
        dontUseApiForLatestRelease: Boolean = false,
        settings: NetworkSettingsHelper
    ): Result {
        check(resultsPerPage > 0)
        val start = if (dontUseApiForLatestRelease) 1 else 0
        var firstReleaseHasAssets = true
        for (tries in start..5) {
            val releases = try {
                val baseUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases"
                if (tries == 0) {
                    val url = "$baseUrl/latest"
                    arrayOf(apiConsumer.consume(url, settings, Release::class))
                } else {
                    val url = "$baseUrl?per_page=$resultsPerPage&page=${tries}"
                    apiConsumer.consume(url, settings, Array<Release>::class)
                }
            } catch (e: NetworkException) {
                throw NetworkException("Fail to request the latest version of $repoName from GitHub.", e)
            }

            releases
                .filter { release -> isValidRelease.test(release) }
                .forEach { release ->
                    release.assets
                        .firstOrNull { asset -> isSuitableAsset.test(asset) }
                        ?.let { asset ->
                            return Result(
                                tagName = release.tagName,
                                url = asset.downloadUrl,
                                fileSizeBytes = asset.fileSizeBytes,
                                releaseDate = release.publishedAt,
                                firstReleaseHasAssets = firstReleaseHasAssets,
                            )
                        }
                    // this will only be called if the first valid release has no valid assets
                    firstReleaseHasAssets = false
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
    ) {
        fun anyAssetNameEndsWith(suffix: String): Boolean {
            return assets.any { it.name.endsWith(suffix) }
        }
    }

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
        val firstReleaseHasAssets: Boolean
    )

    companion object {
        val INSTANCE = GithubConsumer(ApiConsumer.INSTANCE)
    }
}