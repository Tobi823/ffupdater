package de.marmaro.krt.ffupdater.network.mozillaci

import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.Sha256Hash
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Consume the "chain_of_trust.json".
 */
class MozillaCiJsonConsumer(private val apiConsumer: ApiConsumer) {

    suspend fun findTaskId(fileDownloader: FileDownloader, indexPath: String): String {
        val pageUrl = "https://firefox-ci-tc.services.mozilla.com/graphql"
        val requestJson = """{"operationName":"IndexedTask","variables":{"indexPath":"$indexPath"},"query":"query """ +
                """IndexedTask(${'$'}indexPath: String!) {indexedTask(indexPath: ${'$'}indexPath) {taskId}}"}"""
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())
        val responseString = try {
            apiConsumer.consume(pageUrl, fileDownloader, "POST", requestBody)
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

    suspend fun findChainOfTrustJson(fileDownloader: FileDownloader, taskId: String, abiString: String): Result {
        val url = "https://firefoxci.taskcluster-artifacts.net/$taskId/0/public/chain-of-trust.json"
        val json = try {
            fileDownloader.downloadJsonAsync(url)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to get the taskId from graphql API.", e)
        }
        val fileHash = json.asJsonObject["artifacts"]
            .asJsonObject["public/build/fenix/$abiString/target.apk"]
            .asJsonObject["sha256"]
            .asString
        val releaseDate = json.asJsonObject["task"]
            .asJsonObject["created"]
            .asString
        return Result(Sha256Hash(fileHash), releaseDate)
    }

    data class Result(
        val fileHash: Sha256Hash,
        val releaseDate: String,
    )

    companion object {
        val INSTANCE = MozillaCiJsonConsumer(ApiConsumer.INSTANCE)
    }
}
