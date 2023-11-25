package de.marmaro.krt.ffupdater.network.github

import androidx.annotation.Keep
import androidx.annotation.MainThread
import com.google.gson.stream.JsonReader
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import java.util.function.Predicate

@Keep
object GithubConsumer {

    @MainThread
    @Throws(NetworkException::class, IllegalStateException::class)
    suspend fun findLatestRelease(
        repository: GithubRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        cacheBehaviour: CacheBehaviour,
        requireReleaseDescription: Boolean,
    ): Result {
        if (repository.irrelevantReleasesBetweenRelevant == 0) {
            findWithFirstApi(repository, isValidRelease, isSuitableAsset, cacheBehaviour, requireReleaseDescription)
                ?.let { return it } // return if not null
        }

        findWithSecondApi(repository, isValidRelease, isSuitableAsset, cacheBehaviour, requireReleaseDescription)
            ?.let { return it } // return if not null

        throw InvalidApiResponseException("can't find latest release")
    }

    private suspend fun findWithFirstApi(
        repository: GithubRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        cacheBehaviour: CacheBehaviour,
        requireReleaseDescription: Boolean,
    ): Result? {
        val url = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases/latest"
        return try {
            FileDownloader.downloadWithCache(url, cacheBehaviour) {
                val reader = JsonReader(it)
                val c = GithubReleaseJsonConsumer(reader, isValidRelease, isSuitableAsset, requireReleaseDescription)
                c.parseReleaseJson()
            }
        } catch (e: NetworkException) {
            // sometimes GitHub can't determine the latest release and returns 404.
            // solution: return null to retry with findWithSecondApi()
            null
        }
    }

    private suspend fun findWithSecondApi(
        repository: GithubRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        cacheBehaviour: CacheBehaviour,
        requireReleaseDescription: Boolean,
    ): Result? {
        val resultsPerApiCall = (repository.irrelevantReleasesBetweenRelevant + 1)
        for (page in 1..10) {
            val url = "https://api.github.com/repos/${repository.owner}/${repository.name}/releases?" +
                    "per_page=$resultsPerApiCall&page=$page"
            val possibleResult = FileDownloader.downloadWithCache(url, cacheBehaviour) {
                val jsonConsumer = GithubReleaseJsonConsumer(
                    JsonReader(it),
                    isValidRelease,
                    isSuitableAsset,
                    requireReleaseDescription
                )
                jsonConsumer.parseReleaseArrayJson()
            }
            possibleResult?.let { return it }
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
        val releaseDescription: String?,
    )

    @Keep
    data class GithubRepo(val owner: String, val name: String, val irrelevantReleasesBetweenRelevant: Int)
}