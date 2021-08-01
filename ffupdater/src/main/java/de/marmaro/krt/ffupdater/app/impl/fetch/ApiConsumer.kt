package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Consume a REST-API from the internet.
 * TODO make it singleton
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
     * @throws ApiNetworkException if the network resource is not available after 5 retires.
     * @throws GithubRateLimitExceededException if the GitHub-API rate limit is exceeded.
     */
    private suspend fun readNetworkResourceWithRetries(url: URL): BufferedReader {
        val errorMessages = arrayListOf<String>()
        var lastException: Exception? = null

        repeat(5) { i ->
            if (i != 0) {
                delay(NETWORK_RETRY_DELAY)
            }
            try {
                return readNetworkResource(url)
            } catch (e: FileNotFoundException) {
                if (url.host == "api.github.com") {
                    throw GithubRateLimitExceededException(e)
                }
                errorMessages.add(e.message ?: "")
                lastException = e
            } catch (e: IOException) {
                errorMessages.add(e.message ?: "")
                lastException = e
            }
        }

        val previousErrors = errorMessages.joinToString("; ")
        val error = "Failed to consume API. Previous exceptions: [${previousErrors}]."
        throw ApiNetworkException(error, lastException!!)
    }

    /**
     * @throws IOException
     */
    private fun readNetworkResource(url: URL): BufferedReader {
        TrafficStats.setThreadStatsTag(THREAD_ID)
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("Accept-Encoding", GZIP)
        connection.connectTimeout = CONNECTION_TIMEOUT_MS
        return connection.inputStream
            .let { if (connection.contentEncoding == GZIP) GZIPInputStream(it) else it }
            .let { InputStreamReader(it) }
            .let { BufferedReader(it) }
    }

    companion object {
        private const val GZIP = "gzip"
        private const val THREAD_ID = 10000
        private const val CONNECTION_TIMEOUT_MS = 10_000 //10 seconds
        private const val NETWORK_RETRY_DELAY = 5_000L //5 seconds
    }
}