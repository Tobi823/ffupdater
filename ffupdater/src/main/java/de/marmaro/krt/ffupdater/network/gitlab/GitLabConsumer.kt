package de.marmaro.krt.ffupdater.network.gitlab

import androidx.annotation.Keep
import androidx.annotation.MainThread
import com.google.gson.stream.JsonReader
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import java.util.function.Predicate

@Keep
object GitLabConsumer {

    @MainThread
    @Throws(NetworkException::class, IllegalStateException::class)
    suspend fun findLatestRelease(
        repository: GitLabRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        requireReleaseDescription: Boolean,
    ): Result {
        if (repository.irrelevantReleasesBetweenRelevant == 0) {
            findWithFirstApi(repository, isValidRelease, isSuitableAsset, requireReleaseDescription)
                ?.let { return it }
        }

        findWithSecondApi(repository, isValidRelease, isSuitableAsset, requireReleaseDescription)
            ?.let { return it } 

        throw InvalidApiResponseException("can't find latest release")
    }

    private suspend fun findWithFirstApi(
        repository: GitLabRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        requireReleaseDescription: Boolean,
    ): Result? {
        val url = "https://gitlab.com/api/v4/projects/${repository.owner}%2F${repository.name}/releases/permalink/latest"
        return try {
            FileDownloader.downloadAsBufferedReader(url) {
                val reader = JsonReader(it)
                val c = GitLabReleaseJsonConsumer(reader, isValidRelease, isSuitableAsset, requireReleaseDescription)
                c.parseReleaseJson()
            }
        } catch (e: NetworkException) {
            // GitLab API might not return the latest release properly, retry with findWithSecondApi()
            null
        }
    }

    private suspend fun findWithSecondApi(
        repository: GitLabRepo,
        isValidRelease: Predicate<SearchParameterForRelease>,
        isSuitableAsset: Predicate<SearchParameterForAsset>,
        requireReleaseDescription: Boolean,
    ): Result? {
        val resultsPerApiCall = (repository.irrelevantReleasesBetweenRelevant + 1)
        for (page in 1..10) {
            val url = "https://gitlab.com/api/v4/projects/${repository.projectId}/releases/latest" +
                    "per_page=$resultsPerApiCall&page=$page"
            val possibleResult = FileDownloader.downloadAsBufferedReader(url) {
                val jsonConsumer = GitLabReleaseJsonConsumer(
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
    data class GitLabRepo(val owner: String, val name: String, val projectId: Int, val irrelevantReleasesBetweenRelevant: Int)
}
