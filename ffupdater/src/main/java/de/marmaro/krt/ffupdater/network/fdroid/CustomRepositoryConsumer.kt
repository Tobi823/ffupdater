package de.marmaro.krt.ffupdater.network.fdroid

import androidx.annotation.Keep
import androidx.annotation.MainThread
import com.google.gson.JsonObject
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

@Keep
@Deprecated("This was only be used by DivestOS app which will no longer be maintained.")
object CustomRepositoryConsumer {

    @MainThread
    suspend fun getLatestUpdate(
        repoUrl: String,
        packageName: String,
        abi: ABI,
    ): LatestVersion {
        val jsonRoot = FileDownloader.downloadAsJsonObject("$repoUrl/index-v1.json")
        val apkObjects = parseJson(jsonRoot, packageName)
        val apk = apkObjects
            .filter { abi.codeName in it.abis }
            .maxBy { it.versionCode }
        return LatestVersion(
            downloadUrl = "$repoUrl/${apk.apkName}",
            version = Version(apk.versionName),
            publishDate = Instant.ofEpochMilli(apk.added).toString(),
            exactFileSizeBytesOfDownload = apk.size,
            fileHash = Sha256Hash(apk.hash),
        )
    }

    private suspend fun parseJson(
        jsonRoot: JsonObject,
        packageName: String,
    ): List<ApkObject> {
        return try {
            withContext(Dispatchers.Default) {
                val listOfPackages = jsonRoot["packages"].asJsonObject
                val listOfApks = listOfPackages[packageName].asJsonArray
                listOfApks
                    .map { it.asJsonObject }
                    .map {
                        ApkObject(
                            added = it["added"].asLong,
                            apkName = it["apkName"].asString,
                            abis = it["nativecode"].asJsonArray.map { nativecode -> nativecode.asString },
                            hash = it["hash"].asString,
                            size = it["size"].asLong,
                            versionCode = it["versionCode"].asLong,
                            versionName = it["versionName"].asString,
                        )
                    }
            }
        } catch (e: Exception) {
            when (e) {
                is NullPointerException,
                is NumberFormatException,
                is IllegalStateException,
                is UnsupportedOperationException,
                is IndexOutOfBoundsException,
                -> throw NetworkException("Returned JSON is incorrect. Try delete the cache of FFUpdater.", e)
            }
            throw e
        }
    }

    @Keep
    private data class ApkObject(
        val added: Long,
        val apkName: String,
        val abis: List<String>,
        val hash: String,
        val size: Long,
        val versionCode: Long,
        val versionName: String,
    )
}