package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.security.Sha256Hash
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/**
 * Consume the "chain_of_trust.json".
 */
class MozillaCiJsonConsumer(task: String, private val apkArtifact: String) {
    private val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val jsonUrl = "$baseUrl/artifacts/public/chain-of-trust.json"

    /**
     * This method must not be called from the main thread or a android.os.NetworkOnMainThreadException
     * will be thrown
     * @throws NetworkException
     */
    @MainThread
    suspend fun updateCheck(): Result {
        val response = ApiConsumer.consumeNetworkResource(jsonUrl, ChainOfTrustJson::class)
        val artifact = response.artifacts[apkArtifact]
        checkNotNull(artifact) {
            "Missing artifact '$apkArtifact'. Only [${response.artifacts.keys.joinToString()}] " +
                    "are available."
        }
        val releaseDate = ZonedDateTime.parse(response.task.created, ISO_ZONED_DATE_TIME)
        return Result(
            fileHash = Sha256Hash(artifact.sha256),
            url = "$baseUrl/artifacts/$apkArtifact",
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
        val url: String,
        val releaseDate: ZonedDateTime,
    )
}
