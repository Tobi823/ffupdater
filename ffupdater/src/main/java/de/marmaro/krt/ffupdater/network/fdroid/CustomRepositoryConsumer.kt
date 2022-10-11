package de.marmaro.krt.ffupdater.network.fdroid

import android.content.Context
import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.time.Instant

class CustomRepositoryConsumer(
    private val apiConsumer: ApiConsumer,
) {

    @MainThread
    suspend fun getLatestUpdate(
        context: Context,
        repoUrl: String,
        packageName: String,
        abi: ABI
    ): LatestUpdate {
        val mainObject = try {
            apiConsumer.consumeAsync("$repoUrl/index-v1.json", MainObject::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to find the latest version from index-v1.json.", e)
        }

        val packageObject = checkNotNull(mainObject.packages[packageName])
        val apk = packageObject
            // always accept APKs without ABI requirements
            .filter { apkObject -> apkObject.abis?.contains(abi.name) ?: true }
            .maxBy { apkObject -> apkObject.versionCode }

        return LatestUpdate(
            downloadUrl = "$repoUrl/${apk.apkName}",
            version = apk.versionName,
            publishDate = Instant.ofEpochMilli(apk.added).toString(),
            fileSizeBytes = apk.size,
            fileHash = Sha256Hash(apk.hash),
            firstReleaseHasAssets = true
        )
    }

    data class MainObject(
        @SerializedName("packages")
        val packages: Map<String, List<ApkObject>>,
    )

    data class ApkObject(
        @SerializedName("added")
        val added: Long,
        @SerializedName("apkName")
        val apkName: String,
        @SerializedName("nativeCode")
        val abis: List<String>?,
        @SerializedName("hash")
        val hash: String,
        @SerializedName("size")
        val size: Long,
        @SerializedName("versionCode")
        val versionCode: Long,
        @SerializedName("versionName")
        val versionName: String,
    )

    companion object {
        val INSTANCE = CustomRepositoryConsumer(ApiConsumer.INSTANCE)
    }
}