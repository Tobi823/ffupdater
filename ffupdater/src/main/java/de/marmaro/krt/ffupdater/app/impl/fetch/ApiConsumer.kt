package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
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

    /**
     * @throws ApiNetworkException
     */
    suspend fun consumeText(url: URL): String {
        readNetworkResourceWithRetries(url).use {
            return it.readText()
        }
    }

    /**
     * @throws ApiNetworkException
     */
    suspend fun <T> consumeJson(url: URL, clazz: Class<T>): T {
        readNetworkResourceWithRetries(url).use {
            return gson.fromJson(it, clazz)
        }
    }

    /**
     * @throws ApiNetworkException
     */
    private suspend fun readNetworkResourceWithRetries(url: URL): BufferedReader {
        var errorMessages: String
        var lastException: Exception
        try {
            return readNetworkResource(url)
        } catch (e: IOException) {
            lastException = e
            errorMessages = e.message ?: ""
        }

        // if the first try was not successful, retry it again
        repeat(4) {
            delay(5_000L)
            try {
                return readNetworkResource(url)
            } catch (e: IOException) {
                lastException = e
                errorMessages += "; ${e.message}"
            }
        }
        val error = "Fail to consume API. Previous exceptions: [$errorMessages]. Last exception:"
        throw ApiNetworkException(error, lastException)
    }

    /**
     * @throws IOException
     */
    private fun readNetworkResource(url: URL): BufferedReader {
        TrafficStats.setThreadStatsTag(THREAD_ID)
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("Accept-Encoding", GZIP)
        connection.connectTimeout = 10_000 // 10 seconds
        return connection.inputStream
                .let { if (connection.contentEncoding == GZIP) GZIPInputStream(it) else it }
                .let { InputStreamReader(it) }
                .let { BufferedReader(it) }
    }

    companion object {
        private const val GZIP = "gzip"
        private const val THREAD_ID = 10000
    }
}