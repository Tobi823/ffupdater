package de.marmaro.krt.ffupdater.network.mozillaci

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.Sha256Hash
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Consume the "chain_of_trust.json".
 */
@Keep
object MozillaCiJsonConsumer {

    suspend fun findTaskId(indexPath: String, cacheBehaviour: CacheBehaviour): String {
        val pageUrl = "https://firefox-ci-tc.services.mozilla.com/graphql"
        val requestJson = """{"operationName":"IndexedTask","variables":{"indexPath":"$indexPath"},"query":"query """ +
                """IndexedTask(${'$'}indexPath: String!) {indexedTask(indexPath: ${'$'}indexPath) {taskId}}"}"""
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())
        val responseString = try {
            FileDownloader.downloadStringWithCache(pageUrl, cacheBehaviour, "POST", requestBody)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the taskId from graphql API.", e)
        }
        val regex = """taskId":"(?<taskId>[\w-]+)"""".toRegex()
        val matches = regex.find(responseString)
        val taskId = matches?.groups?.get("taskId")?.value
        checkNotNull(taskId) {
            "Missing taskId. Data: $responseString, Matches: $matches."
        }
        return taskId
    }

    suspend fun findChainOfTrustJson(
        taskId: String,
        abiString: String,
        cacheBehaviour: CacheBehaviour,
    ): Result {
        val url = "https://firefoxci.taskcluster-artifacts.net/$taskId/0/public/chain-of-trust.json"
        val json = try {
            FileDownloader.downloadJsonObjectWithCache(url, cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the taskId from graphql API.", e)
        }
        val fileHash = json["artifacts"]
            .asJsonObject["public/build/fenix/$abiString/target.apk"]
            .asJsonObject["sha256"]
            .asString
        val releaseDate = json.asJsonObject["task"]
            .asJsonObject["created"]
            .asString
        return Result(Sha256Hash(fileHash), releaseDate)
    }

    @Keep
    data class Result(
        val fileHash: Sha256Hash,
        val releaseDate: String,
    )
}
