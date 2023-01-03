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

    data class ResultPerPageAndPageNumber(val resultsPerPage: Int, val pageNumber: Int)

    @MainThread
    @Throws(NetworkException::class)
    suspend fun updateCheck(
        repoOwner: String,
        repoName: String,
        initResultsPerPage: Int,
        isValidRelease: Predicate<Release>,
        isSuitableAsset: Predicate<Asset>,
        // false -> contact "$url/latest" and then "$url?per_page=..&page=.."
        // true -> contact only "$url?per_page=..&page=.."
        // set it to true if it is unlikely that the latest release is a valid release
        dontUseApiForLatestRelease: Boolean = false,
        settings: NetworkSettingsHelper,
    ): Result {
        check(initResultsPerPage > 0)
        if (!dontUseApiForLatestRelease) {
            val latestRelease = getLatestRelease(repoOwner, repoName, settings)
            tryToFindResultInReleases(arrayOf(latestRelease), isValidRelease, isSuitableAsset)
                ?.let { return it }
        }

        for (resultsPerPageAndPageNumber in getResultsPerPageAndPageNumbers(initResultsPerPage)) {
            val resultsPerPage = resultsPerPageAndPageNumber.resultsPerPage
            check(resultsPerPage <= 100)
            val pageNumber = resultsPerPageAndPageNumber.pageNumber

            val releases = getReleaseFromPage(repoOwner, repoName, resultsPerPage, pageNumber, settings)
            tryToFindResultInReleases(releases, isValidRelease, isSuitableAsset)
                ?.let { return it }
        }

        throw InvalidApiResponseException("can't find release after all tries - abort")
    }

    private suspend fun getLatestRelease(
        repoOwner: String,
        repoName: String,
        settings: NetworkSettingsHelper,
    ): Release {
        return try {
            val baseUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases"
            apiConsumer.consume("$baseUrl/latest", settings, Release::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $repoName from GitHub.", e)
        }
    }

    private fun getResultsPerPageAndPageNumbers(initResultsPerPage: Int): List<ResultPerPageAndPageNumber> {
        check(initResultsPerPage <= 100)
        val results = mutableListOf(
            ResultPerPageAndPageNumber(initResultsPerPage, 1),
            ResultPerPageAndPageNumber(initResultsPerPage, 2)
        )

        var resultsPerPage = initResultsPerPage
        var pageNumber = 2
        repeat(5) {
            if (resultsPerPage <= 50) {
                resultsPerPage *= 2
            } else {
                pageNumber += 1
            }
            results.add(ResultPerPageAndPageNumber(resultsPerPage, pageNumber))
        }

        return results
    }

    private suspend fun getReleaseFromPage(
        repoOwner: String,
        repoName: String,
        resultsPerPage: Int,
        page: Int,
        settings: NetworkSettingsHelper,
    ): Array<Release> {
        return try {
            val baseUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases"
            val url = "$baseUrl?per_page=$resultsPerPage&page=$page"
            apiConsumer.consume(url, settings, Array<Release>::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $repoName from GitHub.", e)
        }
    }

    private fun tryToFindResultInReleases(
        releases: Array<Release>,
        isValidRelease: Predicate<Release>,
        isSuitableAsset: Predicate<Asset>,
    ): Result? {
        val release = releases
            .filter { isValidRelease.test(it) }
            .firstOrNull { it.assets.any { asset -> isSuitableAsset.test(asset) } }
            ?: return null
        val asset = release.assets.first { asset -> isSuitableAsset.test(asset) }

        return Result(
            tagName = release.tagName,
            url = asset.downloadUrl,
            fileSizeBytes = asset.fileSizeBytes,
            releaseDate = release.publishedAt,
        )
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

        fun anyAssetNameStartsAndEnds(prefix: String, suffix: String): Boolean {
            return assets.any { it.nameStartsOrEnds(prefix, suffix) }
        }
    }

    data class Asset(
        @SerializedName("name")
        val name: String,
        @SerializedName("browser_download_url")
        val downloadUrl: String,
        @SerializedName("size")
        val fileSizeBytes: Long,
    ) {
        fun nameStartsOrEnds(prefix: String, suffix: String): Boolean {
            return name.startsWith(prefix) && name.endsWith(suffix)
        }
    }

    data class Result(
        val tagName: String,
        val url: String,
        val fileSizeBytes: Long,
        val releaseDate: String
    )

    companion object {
        val INSTANCE = GithubConsumer(ApiConsumer.INSTANCE)
    }
}