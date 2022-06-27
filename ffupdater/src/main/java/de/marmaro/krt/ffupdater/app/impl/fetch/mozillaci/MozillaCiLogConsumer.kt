package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer

/**
 * Consume the "chain_of_trust.log".
 */
class MozillaCiLogConsumer(
    task: String,
    private val apkArtifact: String,
    private val apiConsumer: ApiConsumer,
) {
    private val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val logUrl = "$baseUrl/artifacts/public/logs/chain_of_trust.log"

    /**
     * This method must not be called from the main thread or a android.os.NetworkOnMainThreadException
     * will be thrown
     * @throws NetworkException
     */
    @MainThread
    suspend fun updateCheck(): Result {
        val response = apiConsumer.consumeAsync(logUrl, String::class).await()
        val extractVersion = {
            val regexMatch = Regex("""'(version|tag_name)': 'v?(.+)'""")
                .find(response)
            checkNotNull(regexMatch) {
                "Fail to extract the version with regex from string: \"\"\"$response\"\"\""
            }
            val matchGroup = regexMatch.groups[2]
            checkNotNull(matchGroup) {
                "Fail to extract the version value from regex match: \"${regexMatch.value}\""
            }
            matchGroup.value
        }

        val extractDateString = {
            val regexMatch = Regex("""'(published_at|now)': '(.+)'""")
                .find(response)
            checkNotNull(regexMatch) {
                "Fail to extract the date with regex from string: \"\"\"$response\"\"\""
            }
            val matchGroup = regexMatch.groups[2]
            checkNotNull(matchGroup) {
                "Fail to extract the date value from regex match: \"${regexMatch.value}\""
            }
            matchGroup.value
        }

        return Result(
            version = extractVersion(),
            url = "$baseUrl/artifacts/$apkArtifact",
            releaseDate = extractDateString()
        )
    }

    data class Result(
        val version: String,
        val url: String,
        val releaseDate: String,
    )
}