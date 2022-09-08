package de.marmaro.krt.ffupdater.network.mozillaci

import android.content.Context
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.Sha256Hash

/**
 * Consume the "chain_of_trust.json".
 */
class MozillaCiJsonConsumer(private val apiConsumer: ApiConsumer) {

    @MainThread
    suspend fun updateCheck(
        task: String,
        apkArtifact: String,
        context: Context
    ): Result {
        val baseUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
        val jsonUrl = "$baseUrl/artifacts/public/chain-of-trust.json"

        val response = try {
            apiConsumer.consumeAsync(jsonUrl, ChainOfTrustJson::class, context).await()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $task from Mozilla (json).", e)
        }
        val artifact = response.artifacts[apkArtifact]
        checkNotNull(artifact) {
            "Missing artifact '$apkArtifact'. Only [${response.artifacts.keys.joinToString()}] are available."
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

    companion object {
        val INSTANCE = MozillaCiJsonConsumer(ApiConsumer.INSTANCE)
    }
}
