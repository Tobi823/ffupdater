package de.marmaro.krt.ffupdater.utils

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.BEGIN_ARRAY
import com.google.gson.stream.JsonToken.BEGIN_OBJECT
import com.google.gson.stream.JsonToken.BOOLEAN
import com.google.gson.stream.JsonToken.END_ARRAY
import com.google.gson.stream.JsonToken.END_DOCUMENT
import com.google.gson.stream.JsonToken.END_OBJECT
import com.google.gson.stream.JsonToken.NAME
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.NUMBER
import com.google.gson.stream.JsonToken.STRING
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
import java.util.function.Predicate
import kotlin.system.measureNanoTime

class UtilsTest {

    @Test
    fun getVersionAndCodenameForApiLevel_withAndroidR_returnCorrectText() {
        assertEquals("11 (R)", AndroidVersionCodes.getVersionForApiLevel(Build.VERSION_CODES.R))
    }


    @Test
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
                BEGIN_OBJECT -> reader.beginObject()
                BEGIN_ARRAY -> reader.beginArray()
                END_ARRAY -> reader.endArray()
                END_OBJECT -> reader.endObject()
                NAME -> {
                    val nextName = reader.nextName()
                    when (nextName) {
                        "author", "uploader" -> reader.skipValue()
                    }
                }

                STRING -> reader.skipValue()
                NUMBER -> reader.skipValue()
                BOOLEAN -> reader.skipValue()
                NULL -> reader.skipValue()
                END_DOCUMENT -> return
                null -> return
            }


        }

    }

    data class Release(
        val name: String,
        val prerelease: Boolean,
    ) {
        constructor(currentRelease: CurrentRelease) : this(currentRelease.name!!, currentRelease.prerelease!!)
    }

    data class CurrentRelease(
        var name: String? = null,
        var prerelease: Boolean? = null,
        var tagName: String? = null,
        var publishedAt: String? = null,
        var asset: FoundAsset? = null,
    )

    data class Asset(val name: String) {
        constructor(currentAsset: CurrentAsset) : this(currentAsset.name!!)
        constructor(foundAsset: FoundAsset) : this(foundAsset.name)
    }

    data class CurrentAsset(var name: String? = null, var size: Long? = null, var downloadUrl: String? = null)

    data class FoundAsset(
        val name: String,
        val size: Long,
        val downloadUrl: String,
    ) {
        constructor(currentAsset: CurrentAsset) : this(
            currentAsset.name!!, currentAsset.size!!, currentAsset.downloadUrl!!
        )
    }

    data class SearchConditions(
        val correctRelease: Predicate<Release>,
        val correctAsset: Predicate<Asset>,
    )


    @Test
    fun aaag() {
        var json = File("/home/hacker/Desktop/file.txt")
        val reader = JsonReader(json.bufferedReader())

        val searchConditions = SearchConditions(
            correctRelease = { it.name.startsWith("Nightly v1.54.70 (Chromium 114.0.5735.133)") },
            correctAsset = { it.name.startsWith("policy_templates.zip.sha256.asc") }
        )
        handleReleaseArray(reader, searchConditions)
    }

    fun handleReleaseArray(reader: JsonReader, searchConditions: SearchConditions) {
        assert(reader.peek() == BEGIN_ARRAY)
        reader.beginArray()

        while (reader.peek() == BEGIN_OBJECT) {
            handleReleaseObject(reader, searchConditions)
        }

        assert(reader.peek() == END_ARRAY)
        reader.endArray()
    }

    fun handleReleaseObject(reader: JsonReader, searchConditions: SearchConditions) {
        assert(reader.peek() == BEGIN_OBJECT)
        reader.beginObject()

        val currentRelease = CurrentRelease()

        while (reader.peek() == NAME) {
            when (reader.nextName()) {
                "tag_name" -> currentRelease.tagName = reader.nextString()
                "published_at" -> currentRelease.publishedAt = reader.nextString()
                "name" -> currentRelease.name = reader.nextString()
                "prerelease" -> currentRelease.prerelease = reader.nextBoolean()
                "assets" -> {
                    if (currentRelease.name != null && currentRelease.prerelease != null) {
                        val release = Release(currentRelease)
                        if (searchConditions.correctRelease.test(release)) {
                            currentRelease.asset = handleAssetArray(reader, searchConditions)
                        } else {
                            // ignore assets if release is not correct
                            skipNextEntry(reader)
                        }
                    } else {
                        // should not happen, because assets comes after name and prerelease
                        currentRelease.asset = handleAssetArray(reader, searchConditions)
                    }
                }

                else -> skipNextEntry(reader)
            }
        }

        if (currentRelease.name != null && currentRelease.prerelease != null && currentRelease.asset != null) {
            val release = Release(currentRelease)
            val asset = Asset(currentRelease.asset!!)
            if (searchConditions.correctRelease.test(release) && searchConditions.correctAsset.test(asset)) {
                throw Exception("found release")
            }
        }

        assert(reader.peek() == END_OBJECT)
        reader.endObject()
    }

    fun handleAssetArray(reader: JsonReader, searchConditions: SearchConditions): FoundAsset? {
        assert(reader.peek() == BEGIN_ARRAY)
        reader.beginArray()

        var foundAsset: FoundAsset? = null

        while (reader.peek() == BEGIN_OBJECT) {
            // only store, don't abort JsonReader - maybe we have to read more data from JSON
            handleAssetObject(reader, searchConditions)
                ?.let { foundAsset = it }
        }

        assert(reader.peek() == END_ARRAY)
        reader.endArray()
        return foundAsset
    }

    fun handleAssetObject(reader: JsonReader, searchConditions: SearchConditions): FoundAsset? {
        assert(reader.peek() == BEGIN_OBJECT)
        reader.beginObject()

        val currentAsset = CurrentAsset()
        var foundAsset: FoundAsset? = null

        while (reader.peek() == NAME) {
            // process asset
            when (reader.nextName()) {
                "name" -> currentAsset.name = reader.nextString()
                "size" -> currentAsset.size = reader.nextLong()
                "browser_download_url" -> currentAsset.downloadUrl = reader.nextString()
                else -> skipNextEntry(reader)
            }

            // store asset if it matches the given search conditions
            if (currentAsset.name != null && currentAsset.size != null && currentAsset.downloadUrl != null) {
                val asset = Asset(currentAsset)
                if (searchConditions.correctAsset.test(asset)) {
                    // only store, don't abort JsonReader - maybe we have to read more data from JSON
                    foundAsset = FoundAsset(currentAsset)
                }
            }
        }

        assert(reader.peek() == END_OBJECT)
        reader.endObject()
        return foundAsset
    }

    fun skipNextEntry(reader: JsonReader) {
        when (reader.peek()) {
            BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.peek() != END_ARRAY) {
                    if (reader.peek() == NAME) {
                        reader.nextName()
                    }
                    skipNextEntry(reader)
                }
                reader.endArray()
            }

            END_ARRAY -> {}
            BEGIN_OBJECT -> {
                reader.beginObject()
                while (reader.peek() != END_OBJECT) {
                    if (reader.peek() == NAME) {
                        reader.nextName()
                    }
                    skipNextEntry(reader)
                }
                reader.endObject()
            }

            END_OBJECT -> {}
            NAME -> throw IllegalArgumentException("a")
            STRING -> reader.nextString()
            NUMBER -> reader.nextDouble()
            BOOLEAN -> reader.nextBoolean()
            NULL -> reader.nextNull()
            END_DOCUMENT -> {}
            null -> {}
        }

    }
}