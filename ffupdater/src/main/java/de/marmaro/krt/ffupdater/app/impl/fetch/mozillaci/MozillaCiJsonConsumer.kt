package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/**
 * Consume the "chain_of_trust.json".
 */
class MozillaCiJsonConsumer(
        private val apiConsumer: ApiConsumer,
        task: String,
        private val apkArtifact: String,
) {
    private val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val chainOfTrustJsonUrl = URL("$baseUrl/artifacts/public/chain-of-trust.json")

    /**
     * @throws ApiNetworkException
     */
    suspend fun updateCheck(): Result {
        val response = apiConsumer.consumeJson(chainOfTrustJsonUrl, ChainOfTrustJson::class.java)
        val hashString = response.artifacts[apkArtifact]!!.sha256
        val releaseDate = ZonedDateTime.parse(response.task.created, ISO_ZONED_DATE_TIME)
        return Result(
                fileHash = Sha256Hash(hashString),
                url = URL("$baseUrl/artifacts/$apkArtifact"),
                releaseDate = releaseDate
        )
    }

    data class ChainOfTrustJson(
            val artifacts: Map<String, JsonValue>,
            val task: TaskValue
    )

    data class JsonValue(
            val sha256: String
    )

    data class TaskValue(
            val created: String
    )

    data class Result(
            val fileHash: Sha256Hash,
            val url: URL,
            val releaseDate: ZonedDateTime,
    )
}
