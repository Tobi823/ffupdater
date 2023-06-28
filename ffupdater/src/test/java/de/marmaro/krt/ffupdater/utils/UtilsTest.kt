package de.marmaro.krt.ffupdater.utils

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.marmaro.krt.ffupdater.network.ProgressResponseBody
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.gildor.coroutines.okhttp.await
import java.io.File
import kotlin.system.measureNanoTime

class UtilsTest {

    @Test
    fun getVersionAndCodenameForApiLevel_withAndroidR_returnCorrectText() {
        assertEquals("11 (R)", AndroidVersionCodes.getVersionForApiLevel(Build.VERSION_CODES.R))
    }


    @Test
    @Disabled
    fun aaab() {
        val request = Request.Builder()
            .url("https://api.github.com/repos/brave/brave-browser/releases?per_page=40&page=1")

        runBlocking {
            OkHttpClient.Builder()
                .build()
                .newCall(request.build())
                .await()
        }.use {
            val startTime1 = System.nanoTime()
            val stringValue = it.body?.string()
            println("Body.string() took ${(System.nanoTime() - startTime1) / 1000000} ms")

            val startTime2 = System.nanoTime()
            val test = JsonParser.parseString(stringValue)
            println("JsonParser.parseString() took ${(System.nanoTime() - startTime2) / 1000000} ms")

            println(test)
        }
    }


    @Test
    fun aaac() {
        val client = OkHttpClient.Builder().build()
        val indexPath = "mobile.v3.firefox-android.apks.fenix-nightly.latest.arm64-v8a"
        val requestJson =
            """{"operationName":"IndexedTask","variables":{"indexPath":"$indexPath"},"query":"query IndexedTask(${'$'}indexPath: String!) {indexedTask(indexPath: ${'$'}indexPath) {taskId}}"}"""
        println(requestJson)
        val request = Request.Builder()
            .url("https://firefox-ci-tc.services.mozilla.com/graphql")
            .method("POST", requestJson.toRequestBody("application/json".toMediaType()))

        runBlocking {
            client
                .newCall(request.build())
                .await()
                .use { response ->
                    var time = measureNanoTime {
                        val regex = """taskId":"([\w-]+)"""".toRegex()
                        println(response.body?.string())
                        val result = regex.find(response.body!!.string())!!.groups[1]!!.value
                        print(result)
                    }
                    println("Took time ${time / 1000000} ms")


//                    var json: JsonElement? = null
//                    var time = measureNanoTime {
//                        json = JsonParser.parseString(response.body?.string())
//                    }
//                    println("Took time ${time / 1000000} ms")
//
//                    var s: String? = null
//                    time = measureNanoTime {
//                        s = json!!.asJsonObject["data"].asJsonObject["indexedTask"].asJsonObject["taskId"].asString
//                    }
//                    println("Took time ${time / 1000000} ms")
//
//
//                    println(s)
                }
        }
    }

    @Test
    fun aaad() {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("https://firefoxci.taskcluster-artifacts.net/RbGAMKILScm2EWdEkhk8Pw/0/public/chain-of-trust.json")

        val gson = Gson()
        runBlocking {
            client
                .newCall(request.build())
                .await()
                .use { response ->
                    var time = measureNanoTime {
//                        val obj = gson.fromJson(
//                            response.body?.charStream(),
//                            MozillaCiJsonConsumer.ChainOfTrustJson::class.java
//                        )
//                        println(obj.task.created)
                        response.body?.byteStream()?.reader()?.use { byteStream ->
                            val obj = JsonParser.parseReader(byteStream)
                            val result = obj.asJsonObject["task"].asJsonObject["created"].asString
                            val result2 =
                                obj.asJsonObject["artifacts"].asJsonObject["public/build/fenix/arm64-v8a/target.apk"].asJsonObject["sha256"].asString
                            println(result)
                            println(result2)
                        }

                    }
                    println("Took time ${time / 1000000.0} ms")
                }
        }
    }

    @Test
    @Disabled
    fun aaaa() {
        println(File(".", "http_cache").absolutePath)
        File(".", "http_cache").mkdir()
        val prb = ProgressResponseBody()
        val client: OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(prb)
            .build()

        repeat(2) { r ->
            val request = Request.Builder()
                .url("https://api.github.com/repos/Tobi823/ffupdater/releases/latest")
                .cacheControl(
                    CacheControl.FORCE_NETWORK
                )
                .build()

            val startTime = System.nanoTime()
            runBlocking {
                val response = client.newCall(request).await()
                println(response.body?.string()?.substring(0, 100))
            }

            println("$r - took ${(System.nanoTime() - startTime) / 1000000} ms - sleep 1s")
            Thread.sleep(1000)
        }


    }

    @Test
    fun aaaf() {
        var json = File("/home/hacker/Desktop/file.txt")
        val reader = JsonReader(json.bufferedReader())

        while (true) {
            when (reader.peek()) {
                JsonToken.BEGIN_OBJECT -> reader.beginObject()
                JsonToken.BEGIN_ARRAY -> reader.beginArray()
                JsonToken.END_ARRAY -> reader.endArray()
                JsonToken.END_OBJECT -> reader.endObject()
                JsonToken.NAME -> {
                    val nextName = reader.nextName()
                    when (nextName) {
                        "author", "uploader" -> reader.skipValue()
                    }
                }

                JsonToken.STRING -> reader.skipValue()
                JsonToken.NUMBER -> reader.skipValue()
                JsonToken.BOOLEAN -> reader.skipValue()
                JsonToken.NULL -> reader.skipValue()
                JsonToken.END_DOCUMENT -> return
                null -> return
            }


        }

    }

    @Test
    fun aaag() {
        var json = File("/home/hacker/Desktop/file.txt")
        JsonParser.parseReader(json.bufferedReader())

    }
}