package de.marmaro.krt.ffupdater.network.fdroid

import android.content.Context
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException

class FdroidConsumer(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
) {

    @MainThread
    suspend fun getLatestUpdate(packageName: String, context: Context): Result {
        val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"
        val appInfo = try {
            apiConsumer.consumeAsync(apiUrl, AppInfo::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $packageName from F-Droid.", e)
        }

        val versionName = appInfo.packages
            .first { p -> p.versionCode == appInfo.suggestedVersionCode }
            .versionName

        val versionCodesAndUrls = appInfo.packages
            .filter { p -> p.versionName == versionName }
            .sortedBy { p -> p.versionCode }
            .map { p ->
                val downloadUrl = "https://f-droid.org/repo/${packageName}_${p.versionCode}.apk"
                VersionCodeAndDownloadUrl(p.versionCode, downloadUrl)
            }

        val commitId = getLastCommitId(packageName, context)
        val createdAt = getCreateDate(commitId, context)

        return Result(versionName, versionCodesAndUrls, createdAt)
    }

    private suspend fun getLastCommitId(packageName: String, context: Context): String {
        val url = "https://gitlab.com/api/v4/projects/36528/repository/files/metadata%2F${packageName}.yml" +
                "?ref=master"
        val metadata = try {
            apiConsumer.consumeAsync(url, GitlabRepositoryFilesMetadata::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the latest commit id of $packageName.", e)
        }
        return metadata.last_commit_id
    }

    private suspend fun getCreateDate(commitId: String, context: Context): String {
        val url = "https://gitlab.com/api/v4/projects/36528/repository/commits/$commitId"
        val commits = try {
            apiConsumer.consumeAsync(url, GitlabRepositoryCommits::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the creation date of commit $commitId.", e)
        }
        return commits.created_at
    }

    internal data class AppInfo(
        val packageName: String,
        val suggestedVersionCode: Long,
        val packages: List<Package>,
    )

    internal data class Package(
        val versionName: String,
        val versionCode: Long
    )

    data class Result(
        val versionName: String,
        val versionCodesAndDownloadUrls: List<VersionCodeAndDownloadUrl>,
        val createdAt: String,
    )

    data class VersionCodeAndDownloadUrl(
        val versionCode: Long,
        val downloadUrl: String,
    )

    data class GitlabRepositoryFilesMetadata(
        val last_commit_id: String
    )

    data class GitlabRepositoryCommits(
        val created_at: String
    )

    companion object {
        val INSTANCE = FdroidConsumer()
    }
}