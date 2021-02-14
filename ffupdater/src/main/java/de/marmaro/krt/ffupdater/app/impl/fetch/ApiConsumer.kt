package de.marmaro.krt.ffupdater.app.impl.fetch

import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    suspend fun <T> consume(url: URL, clazz: Class<T>): T {
        var errorMessages = ""
        for (i in 1L..2L) {
            try {
                return consumeWithoutRetrying(url, clazz)
            } catch (e: IOException) {
                errorMessages += "; ${e.message}"
            }
            delay(5000 * i)
        }
        try {
            return consumeWithoutRetrying(url, clazz)
        } catch (e: Exception) {
            throw ApiConsumerRetryException("Fail to consume API. " +
                    "Previous exceptions: [$errorMessages]. Current exception:", e)
        }
    }

    private suspend fun <T> consumeWithoutRetrying(url: URL, clazz: Class<T>): T {
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("Accept-Encoding", GZIP)
        connection.connectTimeout = 10 * 1000 // 10 seconds
        connection.inputStream
                .let { if (connection.contentEncoding == GZIP) GZIPInputStream(it) else it }
                .let { InputStreamReader(it) }
                .let { BufferedReader(it) }
                .use {
                    if (clazz == String::class.java) {
                        return it.readText() as T
                    }
                    return Gson().fromJson(it, clazz)
                }
    }

    companion object {
        private const val GZIP = "gzip"
    }

    class ApiConsumerRetryException(message: String, throwable: Throwable) : Exception(message, throwable)
}