package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer

class FdroidConsumer(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
) {

    @MainThread
    suspend fun getLatestUpdate(packageName: String): Result {
        val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"
        val appInfo = apiConsumer.consumeAsync(apiUrl, AppInfo::class).await()

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