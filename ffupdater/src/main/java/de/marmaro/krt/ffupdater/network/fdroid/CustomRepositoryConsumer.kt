package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.time.Instant

object CustomRepositoryConsumer {

    @MainThread
    suspend fun getLatestUpdate(
        repoUrl: String,
        packageName: String,
        abi: ABI,
        cacheBehaviour: CacheBehaviour,
    ): LatestUpdate {
        val jsonRoot = try {
            FileDownloader.downloadJsonObjectWithCache("$repoUrl/index-v1.json", cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to find the latest version from index-v1.json.", e)
        }
        val listOfPackages = jsonRoot["packages"].asJsonObject
        val listOfApks = listOfPackages[packageName].asJsonArray
        val apk = listOfApks
            .map { it.asJsonObject }
            .map {
                ApkObject(
                    added = it["added"].asLong,
                    apkName = it["apkName"].asString,
                    abis = it["nativecode"].asJsonArray.map { it.asString },
                    hash = it["hash"].asString,
                    size = it["size"].asLong,
                    versionCode = it["versionCode"].asLong,
                    versionName = it["versionName"].asString,
                )
            }
            .filter { abi.codeName in it.abis }
            .maxBy { it.versionCode }

        return LatestUpdate(
            downloadUrl = "$repoUrl/${apk.apkName}",
            version = apk.versionName,
            publishDate = Instant.ofEpochMilli(apk.added).toString(),
            exactFileSizeBytesOfDownload = apk.size,
            fileHash = Sha256Hash(apk.hash),
        )
    }

    data class ApkObject(
        val added: Long,
        val apkName: String,
        val abis: List<String>,
        val hash: String,
        val size: Long,
        val versionCode: Long,
        val versionName: String,
    )
}