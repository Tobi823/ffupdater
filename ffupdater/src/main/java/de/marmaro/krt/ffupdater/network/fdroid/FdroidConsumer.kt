package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer

class FdroidConsumer(
    private val packageName: String,
    private val apiConsumer: ApiConsumer,
) {

    @MainThread
    suspend fun updateCheck(): Result {
        val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"
        val appInfo = apiConsumer.consumeAsync(apiUrl, AppInfo::class).await()

        val versionCode = appInfo.suggestedVersionCode
        val versionName = appInfo.packages
            .first { p -> p.versionCode == versionCode }
            .versionName
        val downloadUrl = "https://f-droid.org/repo/us.spotco.fennec_dos_$versionCode.apk"

        return Result(versionName, downloadUrl)
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
        val version: String,
        val url: String,
    )
}