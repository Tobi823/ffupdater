package de.marmaro.krt.ffupdater.network.mozillaci

import androidx.annotation.MainThread
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.Sha256Hash

/**
 * Consume the "chain_of_trust.json".
 */
class MozillaCiJsonConsumer(private val apiConsumer: ApiConsumer) {

    @MainThread
    suspend fun updateCheck(
        taskId: String,
        abiString: String,
        fileDownloader: FileDownloader,
    ): Result {
        val jsonUrl = "https://firefoxci.taskcluster-artifacts.net/${taskId}/0/public/chain-of-trust.json"
        val response = try {
            apiConsumer.consume(jsonUrl, fileDownloader, ChainOfTrustJson::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of $taskId from Mozilla (json).", e)
        }
        val artifact = response.artifacts["public/build/fenix/${abiString}/target.apk"]
        checkNotNull(artifact) {
            "Missing artifact '$abiString'. Only [${response.artifacts.keys.joinToString()}] are available."
        }
        return Result(
            fileHash = Sha256Hash(artifact.sha256),
            releaseDate = response.task.created
        )
    }

    data class ChainOfTrustJson(
        @SerializedName("artifacts")
        val artifacts: Map<String, JsonValue>,
        @SerializedName("task")
        val task: TaskValue,
    )

    data class JsonValue(
        @SerializedName("sha256")
        val sha256: String,
    )

    data class TaskValue(
        @SerializedName("created")
        val created: String,
    )

    data class Result(
        val fileHash: Sha256Hash,
        val releaseDate: String,
    )

    companion object {
        val INSTANCE = MozillaCiJsonConsumer(ApiConsumer.INSTANCE)
    }
}
