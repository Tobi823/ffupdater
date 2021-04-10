package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
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
    private val gson = Gson()

    suspend fun consumeText(url: URL): String {
        var result = ""
        consumeWithNetworkRetries(url) { reader -> result = reader.readText() }
        return result
    }

    suspend fun <T> consumeJson(url: URL, clazz: Class<T>): T {
        var result: T? = null
        consumeWithNetworkRetries(url) { reader -> result = gson.fromJson(reader, clazz) }
        return result!!
    }

    private suspend fun consumeWithNetworkRetries(url: URL, consumer: (BufferedReader) -> Unit) {
        var errorMessages: String
        var lastException: Exception
        try {
            consume(url, consumer)
            return
        } catch (e: IOException) {
            lastException = e
            errorMessages = e.message ?: ""
        }

        // if the first try was not successful, retry it again
        repeat(4) {
            delay(5_000L)
            try {
                consume(url, consumer)
                return
            } catch (e: IOException) {
                lastException = e
                errorMessages += "; ${e.message}"
            }
        }
        val error = "Fail to consume API. Previous exceptions: [$errorMessages]. Last exception:"
        throw ApiConsumerRetryIOException(error, lastException)
    }

    private fun consume(url: URL, consumer: (BufferedReader) -> Any) {
        TrafficStats.setThreadStatsTag(THREAD_ID)
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("Accept-Encoding", GZIP)
        connection.connectTimeout = 10_000 // 10 seconds
        connection.inputStream
                .let { if (connection.contentEncoding == GZIP) GZIPInputStream(it) else it }
                .let { InputStreamReader(it) }
                .let { BufferedReader(it) }
                .use { consumer(it) }
    }

    companion object {
        private const val GZIP = "gzip"
        private const val THREAD_ID = 10000
    }

    class ApiConsumerRetryIOException(message: String, throwable: Throwable) : Exception(message, throwable)
}