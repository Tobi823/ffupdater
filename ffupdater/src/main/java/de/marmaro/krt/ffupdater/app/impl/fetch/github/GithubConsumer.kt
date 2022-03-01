package de.marmaro.krt.ffupdater.app.impl.fetch.github

import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.*
import java.util.function.Predicate

class GithubConsumer(
    private val repoOwner: String,
    private val repoName: String,
    private val resultsPerPage: Int,
    private val isValidRelease: Predicate<Release>,
    private val isCorrectAsset: Predicate<Asset>,
) {
    init {
        check(resultsPerPage > 0)
    }

    @MainThread
    suspend fun updateCheck(): Result {
        val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val release = ApiConsumer.consumeNetworkResource(url, Release::class)
        if (isValidRelease.test(release)) {
            return convert(release)
        }
        return updateCheckAllReleases()
    }

    @MainThread
    suspend fun updateCheckAllReleases(): Result {
        val tries = 4
        for (page in 1..(tries + 1)) {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases" +
                    "?per_page=$resultsPerPage&page=$page"
            val releases = ApiConsumer.consumeNetworkResource(url, Array<Release>::class)
            releases.forEach {
                if (isValidRelease.test(it)) {
                    return convert(it)
                }
            }
        }
        throw InvalidApiResponseException("can't find release after $tries tries - abort")
    }

    /**
     * @throws InvalidApiResponseException
     */
    private fun convert(release: Release): Result {
        val asset = release.assets.firstOrNull { isCorrectAsset.test(it) }
                ?: throw InvalidApiResponseException("${release.name} has no suitable asset")
        return Result(
                tagName = release.tagName,
                url = asset.downloadUrl,
                fileSizeBytes = asset.fileSizeBytes,
                releaseDate = ZonedDateTime.parse(release.publishedAt, ISO_ZONED_DATE_TIME))
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
            val releaseDate: ZonedDateTime,
    )
}