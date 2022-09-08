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

        return Result(versionName, versionCodesAndUrls)
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
    )

    data class VersionCodeAndDownloadUrl(
        val versionCode: Long,
        val downloadUrl: String,
    )

    companion object {
        val INSTANCE = FdroidConsumer()
    }
}