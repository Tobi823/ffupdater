package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiConsumerException
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/**
 * Consume the "chain_of_trust.log".
 */
class MozillaCiLogConsumer(task: String, private val apkArtifact: String) {
    private val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val chainOfTrustLogUrl = "$baseUrl/artifacts/public/logs/chain_of_trust.log"

    /**
     * @throws ApiConsumerException
     */
    fun updateCheck(): Result {
        val response = ApiConsumer.consumeNetworkResource(chainOfTrustLogUrl, String::class)
        val version = Regex("""'(version|tag_name)': 'v?(.+)'""")
            .find(response)!!
            .groups[2]!!.value
        val dateString = Regex("""'(published_at|now)': '(.+)'""")
            .find(response)!!
            .groups[2]!!.value
        return Result(
            version = version,
            url = "$baseUrl/artifacts/$apkArtifact",
            releaseDate = ZonedDateTime.parse(dateString, ISO_ZONED_DATE_TIME)
        )
    }

    data class Result(
        val version: String,
        val url: String,
        val releaseDate: ZonedDateTime,
    )
}