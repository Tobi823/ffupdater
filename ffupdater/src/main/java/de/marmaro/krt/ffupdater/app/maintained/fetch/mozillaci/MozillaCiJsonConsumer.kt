package de.marmaro.krt.ffupdater.app.maintained.fetch.mozillaci

import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.app.maintained.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.Sha256Hash

/**
 * Consume the "chain_of_trust.json".
 */
class MozillaCiJsonConsumer(
    task: String,
    private val apkArtifact: String,
    private val apiConsumer: ApiConsumer,
) {
    private val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val jsonUrl = "$baseUrl/artifacts/public/chain-of-trust.json"

    /**
     * This method must not be called from the main thread or a android.os.NetworkOnMainThreadException
     * will be thrown
     * @throws NetworkException
     */
    @MainThread
    suspend fun updateCheck(): Result {
        val response = apiConsumer.consumeAsync(jsonUrl, ChainOfTrustJson::class).await()
        val artifact = response.artifacts[apkArtifact]
        checkNotNull(artifact) {
            "Missing artifact '$apkArtifact'. Only [${response.artifacts.keys.joinToString()}] " +
                    "are available."
        }
        return Result(
            fileHash = Sha256Hash(artifact.sha256),
            url = "$baseUrl/artifacts/$apkArtifact",
            releaseDate = response.task.created
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
        val releaseDate: String,
    )
}
