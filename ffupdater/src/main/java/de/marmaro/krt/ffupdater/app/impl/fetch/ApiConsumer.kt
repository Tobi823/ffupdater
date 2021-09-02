package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
import android.util.Log
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import kotlin.reflect.KClass

/**
 * Consume a REST-API from the internet.
 */
object ApiConsumer {
    private const val GZIP = "gzip"
    private const val THREAD_ID = 10000
    private const val NETWORK_RETRY_DELAY = 5_000L //5 seconds

    private val gson = Gson()

    /**
     * Reads the network resource.
     * If an error occurs, this method tries two additional times for reading the network resource.
     * If clazz is String, then the network response is returned as a string.
     * If class is not a String, then the network response is parsed as JSON and returned as object.
     * @throws ApiNetworkException if the network resource is not available after 5 retires.
     * @throws GithubRateLimitExceededException if the GitHub-API rate limit is exceeded.
     */
    suspend fun <T: Any> consumeNetworkResource(url: String, clazz: KClass<T>): T {
        val errors = Stack<Exception>()
        repeat(3) { i ->
            try {
                return readNetworkResource(url, clazz)
            } catch (e: FileNotFoundException) {
                Log.e("ApiConsumer", "failed $url: $e")
                if (url.startsWith("https://api.github.com")) {
                    throw GithubRateLimitExceededException(e)
                }
                errors.push(e)
            } catch (e: IOException) {
                Log.e("ApiConsumer", "failed $url: $e")
                errors.push(e)
            }
            if (i != 2) {
                delay(NETWORK_RETRY_DELAY)
            }
        }

        require(!errors.empty())
        val lastException = errors.pop()

        var errorMessage = "Failed to consume network resource. Previous exceptions:"
        while(!errors.empty()) {
            errorMessage += " ${errors.pop().message};"
        }
        throw ApiNetworkException(errorMessage, lastException)
    }

    /**
     * @throws IOException
     */
    private fun <T: Any> readNetworkResource(urlString: String, clazz: KClass<T>): T {
        TrafficStats.setThreadStatsTag(THREAD_ID)
        val url = URL(urlString)
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("Accept-Encoding", GZIP)
        connection.inputStream
            .let { if (connection.contentEncoding == GZIP) GZIPInputStream(it) else it }
            .let { InputStreamReader(it) }
            .let { BufferedReader(it) }
            .use {
                if (clazz == String::class) {
                    @Suppress("UNCHECKED_CAST")
                    return it.readText() as T
                }
                return gson.fromJson(it, clazz.java)
            }
    }
}