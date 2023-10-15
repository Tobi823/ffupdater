package de.marmaro.krt.ffupdater.network.mozillaci

import androidx.annotation.Keep
import com.google.gson.JsonObject
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.Sha256Hash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Consume the "chain_of_trust.json".
 */
@Keep
object MozillaCiJsonConsumer {

    @Throws(IllegalStateException::class)
    suspend fun findTaskId(indexPath: String, cacheBehaviour: CacheBehaviour): String {
        val pageUrl = "https://firefox-ci-tc.services.mozilla.com/graphql"
        val requestJson = """{"operationName":"IndexedTask","variables":{"indexPath":"$indexPath"},"query":"query """ +
                """IndexedTask(${'$'}indexPath: String!) {indexedTask(indexPath: ${'$'}indexPath) {taskId}}"}"""
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())
        val responseString = FileDownloader.downloadStringWithCache(pageUrl, cacheBehaviour, "POST", requestBody)
        val regex = """taskId":"([\w-]+)"""".toRegex()
        val matchResult = checkNotNull(regex.find(responseString)) {
            "Missing taskId. Data: $responseString"
        }
        // get matchGroup for taskId
        val matchGroup = checkNotNull(matchResult.groups[1]) {
            "Missing taskId. Data: $responseString"
        }
        return matchGroup.value
    }

    suspend fun findChainOfTrustJson(
        taskId: String,
        abiString: String,
        cacheBehaviour: CacheBehaviour,
    ): Result {
        val url = "https://firefoxci.taskcluster-artifacts.net/$taskId/0/public/chain-of-trust.json"
        val json = FileDownloader.downloadJsonObjectWithCache(url, cacheBehaviour)
        return parseJson(json, abiString)
    }

    private suspend fun parseJson(json: JsonObject, abiString: String): Result {
        return try {
            withContext(Dispatchers.Default) {
                val fileHash = json["artifacts"]
                    .asJsonObject["public/build/target.${abiString}.apk"]
                    .asJsonObject["sha256"]
                    .asString
                val releaseDate = json.asJsonObject["task"]
                    .asJsonObject["created"]
                    .asString
                Result(Sha256Hash(fileHash), releaseDate)
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
}

@Keep
data class Result(
    val fileHash: Sha256Hash,
    val releaseDate: String,
)

