package de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci

import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import java.net.URL

class MozillaCiConsumer(private val apiConsumer: ApiConsumer) {
    fun consume(task: String, apkArtifact: String): Result {
        val taskUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/$task"
        val artifactsUrl = "$taskUrl/artifacts/public"
        val chainOfTrustUrl = URL("$artifactsUrl/chain-of-trust.json")
        val apkUrl = URL("$artifactsUrl/$apkArtifact")
        val response = apiConsumer.consume(chainOfTrustUrl, Response::class.java)
        return Result(
                timestamp = response.task.created,
                hash = response.artifacts[apkArtifact]!!.hash,
                url = apkUrl)
    }
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