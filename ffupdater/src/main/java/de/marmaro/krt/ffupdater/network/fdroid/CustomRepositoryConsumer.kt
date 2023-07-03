package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.MainThread
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.time.Instant

class CustomRepositoryConsumer(
    private val gson: Gson = Gson(),
) {

    @MainThread
    suspend fun getLatestUpdate(
        fileDownloader: FileDownloader,
        repoUrl: String,
        packageName: String,
        abi: ABI,
    ): LatestUpdate {
        val mainObject = try {
            fileDownloader.downloadObject("$repoUrl/index-v1.json", MainObject::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to find the latest version from index-v1.json.", e)
        }

        val packageObject = checkNotNull(mainObject.packages[packageName])
        val apk = packageObject
            // always accept APKs without ABI requirements
            .filter { apkObject -> apkObject.abis!!.contains(abi.codeName) }
            .maxBy { apkObject -> apkObject.versionCode }

        val version = apk.versionName
        return LatestUpdate(
            downloadUrl = "$repoUrl/${apk.apkName}",
            version = version,
            publishDate = Instant.ofEpochMilli(apk.added).toString(),
            exactFileSizeBytesOfDownload = apk.size,
            fileHash = Sha256Hash(apk.hash),
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
        @SerializedName("nativecode")
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
        val INSTANCE = CustomRepositoryConsumer()
    }
}