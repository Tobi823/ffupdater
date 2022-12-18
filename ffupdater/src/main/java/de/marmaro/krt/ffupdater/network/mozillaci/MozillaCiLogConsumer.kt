package de.marmaro.krt.ffupdater.network.mozillaci

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * Consume the "chain_of_trust.log".
 */
class MozillaCiLogConsumer(private val apiConsumer: ApiConsumer) {

    @MainThread
    suspend fun updateCheck(
        task: String,
        apkArtifact: String,
        settings: NetworkSettingsHelper
    ): Result {
        val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
        val logUrl = "$baseUrl/artifacts/public/logs/chain_of_trust.log"

        val response = try {
            apiConsumer.consumeAsync(logUrl, settings, String::class).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $task from Mozilla (log).", e)
        }
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

    companion object {
        val INSTANCE = MozillaCiLogConsumer(ApiConsumer.INSTANCE)
    }
}