package de.marmaro.krt.ffupdater.network.github

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import java.util.*
import java.util.function.Predicate

class GithubConsumer {

    @MainThread
    @Throws(NetworkException::class)
    suspend fun findLatestRelease(
        repository: GithubRepo,
        resultsPerApiCall: Int,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        dontUseApiForLatestRelease: Boolean = false,
        fileDownloader: FileDownloader,
    ): Result {
        check(resultsPerApiCall > 0)
        if (!dontUseApiForLatestRelease) {
            findWithFirstApi(repository, isValidRelease, isSuitableAsset, fileDownloader)
                ?.let { return it } // return if not null
        }

        findWithSecondApi(repository, resultsPerApiCall, isValidRelease, isSuitableAsset, fileDownloader)
            ?.let { return it } // return if not null

        throw InvalidApiResponseException("can't find latest release")
    }

    private suspend fun findWithFirstApi(
        repository: GithubRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        fileDownloader: FileDownloader,
    ): Result? {
        val url = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases/latest"
        fileDownloader.downloadSmallFile(url).use {
            val jsonConsumer = GithubReleaseJsonConsumer(it, isValidRelease, isSuitableAsset)
            return jsonConsumer.parseReleaseJson()
        }
    }

    private suspend fun findWithSecondApi(
        repository: GithubRepo,
        resultsPerApiCall: Int,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        fileDownloader: FileDownloader,
    ): Result? {
        for (page in 1..10) {
            val url = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases?" +
                    "per_page=$resultsPerApiCall&page=$page"
            fileDownloader.downloadSmallFile("$url?per_page=$resultsPerApiCall&page=$page").use {
                val jsonConsumer = GithubReleaseJsonConsumer(it, isValidRelease, isSuitableAsset)
                jsonConsumer.parseReleaseArrayJson()
                    ?.let { result -> return result } // return if not null
            }
        }
        return null
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