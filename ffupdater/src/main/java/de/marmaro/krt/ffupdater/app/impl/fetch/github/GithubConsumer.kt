package de.marmaro.krt.ffupdater.app.impl.fetch.github

import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.*
import java.util.function.Predicate

class GithubConsumer(
        private val apiConsumer: ApiConsumer,
        private val repoOwner: String,
        private val repoName: String,
        private val resultsPerPage: Int,
        private val validReleaseTester: Predicate<Release>,
        private val correctDownloadUrlTester: Predicate<Asset>,
) {
    init {
        check(resultsPerPage > 0)
    }

    suspend fun updateCheck(): Result {
        return updateCheckLatestRelease() ?: updateCheckAllReleases()
    }

    private suspend fun updateCheckLatestRelease(): Result? {
        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val release = apiConsumer.consumeJson(url, Release::class.java)
        return release.takeIf { validReleaseTester.test(it) }?.let { convert(it) }
    }

    private suspend fun updateCheckAllReleases(): Result {
        val tries = 4
        for (page in 1..(tries + 1)) {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases?" +
                    "per_page=$resultsPerPage&page=$page")
            val releases = apiConsumer.consumeJson(url, Array<Release>::class.java)
            releases.firstOrNull { validReleaseTester.test(it) }?.let { return convert(it) }
        }
        throw GithubConsumerException("can't find release after $tries tries - abort")
    }

    private fun convert(release: Release): Result {
        val asset = release.assets.firstOrNull { correctDownloadUrlTester.test(it) }
                ?: throw NoSuitableAssetAvailableException("${release.name} has no suitable asset")
        return Result(
                tagName = release.tagName,
                url = URL(asset.downloadUrl),
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
            val url: URL,
            val fileSizeBytes: Long,
            val releaseDate: ZonedDateTime,
    )

    class GithubConsumerException(message: String) : Exception(message)

    class NoSuitableAssetAvailableException(message: String) : Exception(message)
}