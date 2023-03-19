package de.marmaro.krt.ffupdater.utils

import android.os.Build
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.network.ProgressResponseBody
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.gildor.coroutines.okhttp.await
import java.io.File

class UtilsTest {

    @Test
    fun getVersionAndCodenameForApiLevel_withAndroidR_returnCorrectText() {
        assertEquals("11 (R)", AndroidVersionCodes.getVersionForApiLevel(Build.VERSION_CODES.R))
    }


    @Test
    fun aaab() {
        App.values().sortedBy { app ->
            if (app != App.FFUPDATER) app.ordinal else Int.MAX_VALUE
        }.forEach {
            println(it)
        }
    }

    @Test
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