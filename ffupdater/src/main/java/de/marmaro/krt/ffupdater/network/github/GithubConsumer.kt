package de.marmaro.krt.ffupdater.network.github

import androidx.annotation.Keep
import androidx.annotation.MainThread
import com.google.gson.stream.JsonReader
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import java.util.*
import java.util.function.Predicate

@Keep
object GithubConsumer {

    @MainThread
    @Throws(NetworkException::class)
    suspend fun findLatestRelease(
        repository: GithubRepo,
        resultsPerApiCall: Int,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        dontUseApiForLatestRelease: Boolean = false,
        cacheBehaviour: CacheBehaviour,
    ): Result {
        check(resultsPerApiCall > 0)
        if (!dontUseApiForLatestRelease) {
            findWithFirstApi(repository, isValidRelease, isSuitableAsset, cacheBehaviour)
                ?.let { return it } // return if not null
        }

        findWithSecondApi(repository, resultsPerApiCall, isValidRelease, isSuitableAsset, cacheBehaviour)
            ?.let { return it } // return if not null

        throw InvalidApiResponseException("can't find latest release")
    }

    private suspend fun findWithFirstApi(
        repository: GithubRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        cacheBehaviour: CacheBehaviour,
    ): Result? {
        val url = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases/latest"
        FileDownloader.downloadWithCache(url, cacheBehaviour).charStream().buffered().use {
            val jsonConsumer = GithubReleaseJsonConsumer(JsonReader(it), isValidRelease, isSuitableAsset)
            return jsonConsumer.parseReleaseJson()
        }
    }

    private suspend fun findWithSecondApi(
        repository: GithubRepo,
        resultsPerApiCall: Int,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        cacheBehaviour: CacheBehaviour,
    ): Result? {
        for (page in 1..10) {
            val url = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases?" +
                    "per_page=$resultsPerApiCall&page=$page"
            FileDownloader.downloadWithCache(url, cacheBehaviour).charStream().buffered().use {
                val jsonConsumer = GithubReleaseJsonConsumer(JsonReader(it), isValidRelease, isSuitableAsset)
                jsonConsumer.parseReleaseArrayJson()
                    ?.let { result -> return result } // return if not null
            }
        }
        return null
    }

    @Keep
    data class SearchParameterForRelease(val name: String, val isPreRelease: Boolean)

    @Keep
    data class SearchParameterForAsset(val name: String) {
        fun nameStartsAndEndsWith(prefix: String, suffix: String): Boolean {
            return name.startsWith(prefix) && name.endsWith(suffix)
        }
    }

    @Keep
    data class Result(
        val tagName: String,
        val url: String,
        val fileSizeBytes: Long,
        val releaseDate: String,
    )

    @Keep
    data class GithubRepo(val owner: String, val name: String)

    val REPOSITORY__MOZILLA_MOBILE__FIREFOX_ANDROID = GithubRepo("mozilla-mobile", "firefox-android")
    val REPOSITORY__BRAVE__BRAVE_BROWSER = GithubRepo("brave", "brave-browser")
    val REPOSITORY__BROMITE__BROMITE = GithubRepo("bromite", "bromite")
    const val RESULTS_PER_API_CALL__FIREFOX_ANDROID = 20
    const val RESULTS_PER_API_CALL__BRAVE_BROWSER = 40
    const val RESULTS_PER_API_CALL__BROMITE = 5
}