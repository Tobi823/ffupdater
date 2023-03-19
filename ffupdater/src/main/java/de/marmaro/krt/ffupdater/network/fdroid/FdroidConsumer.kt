package de.marmaro.krt.ffupdater.network.fdroid

import android.util.Log
import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.AddAppActivity.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException

class FdroidConsumer(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
) {

    @MainThread
    suspend fun getLatestUpdate(packageName: String, fileDownloader: FileDownloader, index: Int): Result {
        require(index >= 1)
        val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"
        val appInfo = try {
            apiConsumer.consume(apiUrl, fileDownloader, AppInfo::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $packageName from F-Droid.", e)
        }
        val latestVersions = getLatestVersionsSortedByTheirCode(appInfo)
        check(latestVersions.size >= index)
        val latestVersion = latestVersions[index - 1]

        val commitId = getLastCommitId(packageName, fileDownloader)
        val createdAt = getCreateDate(commitId, fileDownloader)

        Log.i(LOG_TAG, "found latest version ${latestVersion.versionName}")
        return Result(
            latestVersion.versionName,
            latestVersion.versionCode,
            "https://f-droid.org/repo/${packageName}_${latestVersion.versionCode}.apk",
            createdAt
        )
    }

    private fun getLatestVersionsSortedByTheirCode(appInfo: AppInfo): List<Package> {
        val latestVersionCode = appInfo.packages
            .maxOf { p -> p.versionCode }
        val latestVersionName = appInfo.packages
            .firstOrNull { p -> p.versionCode == latestVersionCode }
            ?.versionName
            ?: throw Exception("Can't find version with code $latestVersionCode")
        return appInfo.packages
            .filter { p -> p.versionName == latestVersionName }
            .sortedBy { p -> p.versionCode }
    }

    private suspend fun getLastCommitId(packageName: String, fileDownloader: FileDownloader): String {
        val url = "https://gitlab.com/api/v4/projects/36528/repository/files/metadata%2F${packageName}.yml" +
                "?ref=master"
        val metadata = try {
            apiConsumer.consume(url, fileDownloader, GitlabRepositoryFilesMetadata::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the latest commit id of $packageName.", e)
        }
        return metadata.last_commit_id
    }

    private suspend fun getCreateDate(commitId: String, fileDownloader: FileDownloader): String {
        val url = "https://gitlab.com/api/v4/projects/36528/repository/commits/$commitId"
        val commits = try {
            apiConsumer.consume(url, fileDownloader, GitlabRepositoryCommits::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the creation date of commit $commitId.", e)
        }
        return commits.created_at
    }

    internal data class AppInfo(
        @SerializedName("packageName")
        val packageName: String,
        @SerializedName("suggestedVersionCode")
        val suggestedVersionCode: Long,
        @SerializedName("packages")
        val packages: List<Package>,
    )

    internal data class Package(
        @SerializedName("versionName")
        val versionName: String,
        @SerializedName("versionCode")
        val versionCode: Long,
    )

    data class Result(
        val versionName: String,
        val versionCode: Long,
        val downloadUrl: String,
        val createdAt: String,
    )

    data class GitlabRepositoryFilesMetadata(
        @SerializedName("last_commit_id")
        val last_commit_id: String,
    )

    data class GitlabRepositoryCommits(
        @SerializedName("created_at")
        val created_at: String,
    )

    companion object {
        val INSTANCE = FdroidConsumer()
    }
}