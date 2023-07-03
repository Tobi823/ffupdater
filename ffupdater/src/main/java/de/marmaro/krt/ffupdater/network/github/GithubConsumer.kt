package de.marmaro.krt.ffupdater.network.github

import androidx.annotation.MainThread
import com.google.gson.stream.JsonReader
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import java.util.*
import java.util.function.Predicate

class GithubConsumer {

    @MainThread
    @Throws(NetworkException::class)
    suspend fun updateCheck(
        repository: GithubRepo,
        resultsPerApiCall: Int,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        // false -> contact "$url/latest" and then "$url?per_page=..&page=.."
        // true -> contact only "$url?per_page=..&page=.."
        // set it to true if it is unlikely that the latest release is a valid release
        dontUseApiForLatestRelease: Boolean = false,
        fileDownloader: FileDownloader,
    ): Result {
        check(resultsPerApiCall > 0)
        val baseUrl = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases"
        if (!dontUseApiForLatestRelease) {
            fileDownloader.downloadSmallFile("$baseUrl/latest").use {
                val reader = JsonReader(it.charStream().buffered())
                val jsonConsumer = GithubReleaseJsonConsumer(reader, isValidRelease, isSuitableAsset)
                val result = jsonConsumer.parseReleaseJson()
                result?.let { return it }
            }
        }

        for (page in 1..10) {
            fileDownloader.downloadSmallFile("$baseUrl?per_page=$resultsPerApiCall&page=$page").use {
                val reader = JsonReader(it.charStream().buffered())
                val jsonConsumer = GithubReleaseJsonConsumer(reader, isValidRelease, isSuitableAsset)
                val result = jsonConsumer.parseReleaseArrayJson()
                result?.let { return it }
            }
        }

        throw InvalidApiResponseException("can't find release after all tries - abort")
    }

    data class SearchParameterForRelease(
        val name: String,
        val isPreRelease: Boolean,
    )

    data class SearchParameterForAsset(
        val name: String,
    ) {
        fun nameStartsAndEndsWith(prefix: String, suffix: String): Boolean {
            return name.startsWith(prefix) && name.endsWith(suffix)
        }
    }

    data class Result(
        val tagName: String,
        val url: String,
        val fileSizeBytes: Long,
        val releaseDate: String,
    )

    data class GithubRepo(
        val owner: String,
        val name: String,
    )

    companion object {
        val INSTANCE = GithubConsumer()
        val REPOSITORY__MOZILLA_MOBILE__FIREFOX_ANDROID = GithubRepo("mozilla-mobile", "firefox-android")
        val REPOSITORY__BRAVE__BRAVE_BROWSER = GithubRepo("brave", "brave-browser")
        val REPOSITORY__BROMITE__BROMITE = GithubRepo("bromite", "bromite")
        const val RESULTS_PER_API_CALL__FIREFOX_ANDROID = 20
        const val RESULTS_PER_API_CALL__BRAVE_BROWSER = 40
        const val RESULTS_PER_API_CALL__BROMITE = 5
    }
}