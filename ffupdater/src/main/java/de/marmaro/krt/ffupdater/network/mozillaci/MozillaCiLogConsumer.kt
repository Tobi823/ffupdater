package de.marmaro.krt.ffupdater.network.mozillaci

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException

/**
 * Consume the "chain_of_trust.log".
 */
class MozillaCiLogConsumer {

    @MainThread
    suspend fun updateCheck(
        taskId: String,
        fileDownloader: FileDownloader,
    ): Result {
        val logUrl = "https://firefoxci.taskcluster-artifacts.net/${taskId}/0/public/logs/chain_of_trust.log"
        val response = try {
            fileDownloader.downloadSmallFileAsString(logUrl)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $taskId from Mozilla (log).", e)
        }
        val extractVersion = {
            val regexMatch = Regex("""'(version|tag_name)': 'v?(?<version>.+)'""")
                .find(response)
            checkNotNull(regexMatch) {
                "Fail to extract the version with regex from string: $response"
            }
            val matchGroup = regexMatch.groups["version"]
            checkNotNull(matchGroup) {
                "Fail to extract the version value from regex match: ${regexMatch.value}"
            }
            matchGroup.value
        }

        val extractDateString = {
            val regexMatch = Regex("""'(published_at|now)': '(?<date>.+)'""")
                .find(response)
            checkNotNull(regexMatch) {
                "Fail to extract the date with regex from string: $response"
            }
            val matchGroup = regexMatch.groups["date"]
            checkNotNull(matchGroup) {
                "Fail to extract the date value from regex match: ${regexMatch.value}"
            }
            matchGroup.value
        }

        return Result(
            version = extractVersion(),
            releaseDate = extractDateString()
        )
    }

    data class Result(
        val version: String,
        val releaseDate: String,
    )

    companion object {
        val INSTANCE = MozillaCiLogConsumer()
    }
}