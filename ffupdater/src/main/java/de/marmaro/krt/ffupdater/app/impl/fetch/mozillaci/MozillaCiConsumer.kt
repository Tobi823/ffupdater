package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class MozillaCiConsumer(
        private val apiConsumer: ApiConsumer,
        task: String,
        apkArtifact: String,
        private val keyForVersion: String,
        private val keyForReleaseDate: String,
) {
    private val taskUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val chainOfTrustLogUrl = URL("$taskUrl/artifacts/public/logs/chain_of_trust.log")
    private val apkArtifactUrl = URL("$taskUrl/artifacts/$apkArtifact")

    suspend fun updateCheck(): Result {
        val response = apiConsumer.consumeText(chainOfTrustLogUrl)
        val version = Regex("""'$keyForVersion': '(.+)'""").find(response)!!
                .groups[1]!!.value
        val dateString = Regex("""'$keyForReleaseDate': '(.+)'""").find(response)!!
                .groups[1]!!.value
        return Result(
                version = version,
                url = apkArtifactUrl,
                releaseDate = ZonedDateTime.parse(dateString, ISO_ZONED_DATE_TIME))
    }

    data class Result(
            val version: String,
            val url: URL,
            val releaseDate: ZonedDateTime,
    )
}