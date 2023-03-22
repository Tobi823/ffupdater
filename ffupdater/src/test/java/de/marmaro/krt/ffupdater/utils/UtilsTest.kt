package de.marmaro.krt.ffupdater.utils

import android.os.Build
import com.google.gson.JsonParser
import de.marmaro.krt.ffupdater.network.ProgressResponseBody
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.gildor.coroutines.okhttp.await
import java.io.File

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
}