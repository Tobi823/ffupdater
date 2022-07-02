package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer

class FdroidConsumer(
    private val packageName: String,
    private val apiConsumer: ApiConsumer,
) {

    @MainThread
    suspend fun updateCheck(): List<Result> {
        val apiUrl = "https://f-droid.org/api/v1/packages/$packageName"
        val appInfo = apiConsumer.consumeAsync(apiUrl, AppInfo::class).await()

        val versionCode = appInfo.suggestedVersionCode
        val versionName = appInfo.packages
            .first { p -> p.versionCode == versionCode }
            .versionName

        return appInfo.packages
            .filter { p -> p.versionName == versionName }
            .sortedBy { p -> p.versionCode }
            .map { p ->
                Result(
                    versionName = p.versionName,
                    versionCode = p.versionCode,
                    url = "https://f-droid.org/repo/us.spotco.fennec_dos_${p.versionCode}.apk"
                )
            }
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
        val versionCode: Long,
        val url: String,
    )
}