package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.net.URL

class MozillaCiConsumer(private val apiConsumer: ApiConsumer,
                        task: String,
                        private val apkArtifact: String) {
    private val taskUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
    private val chainOfTrustUrl = URL("$taskUrl/artifacts/public/chain-of-trust.json")
    private val apkArtifactUrl = URL("$taskUrl/artifacts/$apkArtifact")

    fun updateCheck(): Result {
        val response = apiConsumer.consume(chainOfTrustUrl, Response::class.java)
        return Result(
                timestamp = response.task.created,
                hash = response.artifacts[apkArtifact]!!.hash,
                url = apkArtifactUrl)
    }

    data class Response(
            @SerializedName("artifacts")
            val artifacts: Map<String, Sha256Hash>,
            @SerializedName("task")
            val task: Task)

    data class Sha256Hash(
            @SerializedName("sha256")
            var hash: String)

    data class Task(
            @SerializedName("created")
            var created: String)

    data class Result(
            val timestamp: String,
            val hash: String,
            val url: URL)
}