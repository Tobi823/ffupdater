package de.marmaro.krt.ffupdater.app.impl.fetch.github

import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
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
        private val validReleaseTester: Predicate<Release>,
        private val correctDownloadUrlTester: Predicate<Asset>,
) {
    init {
        check(resultsPerPage > 0)
    }

    /**
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     */
    suspend fun updateCheckReliableOnlyForNormalReleases(): Result {
        return updateCheckLatestRelease() ?: updateCheckAllReleases()
    }

    /**
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     */
    private suspend fun updateCheckLatestRelease(): Result? {
        val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val release = ApiConsumer.consumeNetworkResource(url, Release::class)
        return release.takeIf { validReleaseTester.test(it) }?.let { convert(it) }
    }

    /**
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     */
    private suspend fun updateCheckAllReleases(): Result {
        val tries = 4
        for (page in 1..(tries + 1)) {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases" +
                    "?per_page=$resultsPerPage&page=$page"
            val releases = ApiConsumer.consumeNetworkResource(url, Array<Release>::class)
            releases.firstOrNull { validReleaseTester.test(it) }?.let { return convert(it) }
        }
        throw InvalidApiResponseException("can't find release after $tries tries - abort")
    }

    /**
     * @throws InvalidApiResponseException
     */
    private fun convert(release: Release): Result {
        val asset = release.assets.firstOrNull { correctDownloadUrlTester.test(it) }
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