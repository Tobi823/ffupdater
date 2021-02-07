package de.marmaro.krt.ffupdater.app.impl.fetch.github

import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException
import java.net.URL
import java.util.*
import java.util.function.Predicate

class GithubConsumer(private val apiConsumer: ApiConsumer,
                     private val repoOwner: String,
                     private val repoName: String,
                     private val resultsPerPage: Int,
                     private val validReleaseTester: Predicate<Release>,
                     private val correctDownloadUrlTester: Predicate<Asset>) {
    init {
        check(resultsPerPage > 0)
    }

    fun updateCheck(): Result {
        return updateCheckLatestRelease() ?: updateCheckAllReleases()
    }

    private fun updateCheckLatestRelease(): Result? {
        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val release = apiConsumer.consume(url, Release::class.java)
        return release.takeIf { validReleaseTester.test(it) }?.let { convert(it) }
    }

    private fun updateCheckAllReleases(): Result {
        val tries = 4
        for (page in 1..(tries + 1)) {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases?" +
                    "per_page=$resultsPerPage&page=$page")
            val releases = apiConsumer.consume(url, Array<Release>::class.java)
            releases.firstOrNull { validReleaseTester.test(it) }?.let { return convert(it) }
        }
        throw ParamRuntimeException("can't find release after $tries tries - abort")
    }

    private fun convert(release: Release): Result {
        // NoSuchElementException: The valid release doesn't have an asset with the correct download url
        release.assets.first { correctDownloadUrlTester.test(it) }
                .let { return Result(release.tagName, URL(it.downloadUrl), it.fileSizeBytes) }
    }
}

data class Release(
        @SerializedName("tag_name")
        val tagName: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("prerelease")
        val isPreRelease: Boolean,
        @SerializedName("assets")
        val assets: List<Asset>)

data class Asset(
        @SerializedName("name")
        val name: String,
        @SerializedName("browser_download_url")
        val downloadUrl: String,
        @SerializedName("size")
        val fileSizeBytes: Long)

data class Result(
        val tagName: String,
        val url: URL,
        val fileSizeBytes: Long)