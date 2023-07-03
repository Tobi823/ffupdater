package de.marmaro.krt.ffupdater.network.fdroid

import android.util.Log
import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.AddAppActivity.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader

object FdroidConsumer {

    @MainThread
    suspend fun getLatestUpdate(packageName: String, index: Int, cacheBehaviour: CacheBehaviour): Result {
        require(index >= 1)
        val url = "https://f-droid.org/api/v1/packages/$packageName"
        val appInfo = try {
            FileDownloader.downloadObjectWithCache(url, AppInfo::class, cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $packageName from F-Droid.", e)
        }

        val latestVersion = getLatestUpdate(appInfo, index)
        val commitId = getLastCommitId(packageName, cacheBehaviour)
        val createdAt = getCreateDate(commitId, cacheBehaviour)

        Log.i(LOG_TAG, "found latest version ${latestVersion.versionName}")
        return Result(
            latestVersion.versionName,
            latestVersion.versionCode,
            "https://f-droid.org/repo/${packageName}_${latestVersion.versionCode}.apk",
            createdAt
        )
    }

    /**
     * For FennecFdroid two different versions with the same version number are released.
     * The first version is for ARMEABI_V7A devices and the second version for ARM64_V8A.
     * This method helps to extract a specific version from the APi response.
     */
    private fun getLatestUpdate(appInfo: AppInfo, index: Int): Package {
        val latestVersionCode = appInfo.packages
            .maxOf { p -> p.versionCode }
        val latestVersionName = appInfo.packages
            .firstOrNull { p -> p.versionCode == latestVersionCode }
            ?.versionName
            ?: throw Exception("Can't find version with code $latestVersionCode")
        val latestVersions = appInfo.packages
            .filter { p -> p.versionName == latestVersionName }
            .sortedBy { p -> p.versionCode }

        check(latestVersions.size >= index)
        return latestVersions[index - 1]
    }

    private suspend fun getLastCommitId(packageName: String, cacheBehaviour: CacheBehaviour): String {
        val url = "https://gitlab.com/api/v4/projects/36528/repository/files/metadata%2F${packageName}.yml?ref=master"
        val metadata = try {
            FileDownloader.downloadObjectWithCache(url, GitlabRepositoryFilesMetadata::class, cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the latest commit id of $packageName.", e)
        }
        return metadata.last_commit_id
    }

    private suspend fun getCreateDate(commitId: String, cacheBehaviour: CacheBehaviour): String {
        val url = "https://gitlab.com/api/v4/projects/36528/repository/commits/$commitId"
        val commits = try {
            FileDownloader.downloadObjectWithCache(url, GitlabRepositoryCommits::class, cacheBehaviour)
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
}