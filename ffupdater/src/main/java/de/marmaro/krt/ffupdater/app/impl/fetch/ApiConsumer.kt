package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiConsumerException
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import kotlin.reflect.KClass

/**
 * Consume a REST-API from the internet.
 */
object ApiConsumer {
    private const val GZIP = "gzip"
    private const val THREAD_ID = 10000
    private val gson = Gson()

    /**
     * Reads the network resource.
     * If an error occurs, this method tries two additional times for reading the network resource.
     * If clazz is String, then the network response is returned as a string.
     * If class is not a String, then the network response is parsed as JSON and returned as object.
     * @throws ApiConsumerException if the network resource is not available after 5 retires.
     * @throws GithubRateLimitExceededException if the GitHub-API rate limit is exceeded.
     */
    @MainThread
    suspend fun <T: Any> consumeNetworkResource(url: String, clazz: KClass<T>): T {
        try {
            return withContext(Dispatchers.IO) {
                readNetworkResource(url, clazz)
            }
        } catch (e: FileNotFoundException) {
            if (url.startsWith("https://api.github.com")) {
                throw GithubRateLimitExceededException(e)
            } else {
                throw ApiConsumerException("fail to consume ${clazz.qualifiedName} from '$url'", e)
            }
        } catch (e: IOException) {
            throw ApiConsumerException("fail to consume ${clazz.qualifiedName} from '$url'", e)
        }
    }

    @WorkerThread
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