package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/**
 * Consume the "chain_of_trust.log".
 */
class MozillaCiLogConsumer(
        private val apiConsumer: ApiConsumer,
        task: String,
        private val apkArtifact: String,
        private val keyForVersion: String,
        private val keyForReleaseDate: String,
) {
    private val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val chainOfTrustLogUrl = URL("$baseUrl/artifacts/public/logs/chain_of_trust.log")

    /**
     * @throws ApiNetworkException
     */
    suspend fun updateCheck(): Result {
        val response = apiConsumer.consumeText(chainOfTrustLogUrl)
        val version = Regex("""'$keyForVersion': '(.+)'""")
                .find(response)!!
                .groups[1]!!.value
        val dateString = Regex("""'$keyForReleaseDate': '(.+)'""")
                .find(response)!!
                .groups[1]!!.value
        return Result(
                version = version,
                url = URL("$baseUrl/artifacts/$apkArtifact"),
                releaseDate = ZonedDateTime.parse(dateString, ISO_ZONED_DATE_TIME))
    }

    data class Result(
            val version: String,
            val url: URL,
            val releaseDate: ZonedDateTime,
    )
}