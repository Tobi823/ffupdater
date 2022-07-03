package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer

class FdroidConsumer(
    private val packageName: String,
    private val apiConsumer: ApiConsumer,
) {
    private val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"

    @MainThread
    suspend fun updateCheck(): Result {
        val appInfo = apiConsumer.consumeAsync(apiUrl, AppInfo::class).await()

        val versionName = appInfo.packages
            .first { p -> p.versionCode == appInfo.suggestedVersionCode }
            .versionName

        val versionCodesAndUrls = appInfo.packages
            .filter { p -> p.versionName == versionName }
            .sortedBy { p -> p.versionCode }
            .map { p -> VersionCodeAndDownloadUrl(p.versionCode, getDownloadUrl(p.versionCode)) }

        return Result(versionName, versionCodesAndUrls)
    }

    private fun getDownloadUrl(versionCode: Long): String {
        return "https://f-droid.org/repo/${packageName}_${versionCode}.apk"
    }

    data class AppInfo(
        val packageName: String,
        val suggestedVersionCode: Long,
        val packages: List<Package>,
    )

    data class Package(
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
}