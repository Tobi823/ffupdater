package de.marmaro.krt.ffupdater.app.network

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.exceptions.NetworkException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.reflect.KClass


/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    private val threadId = 10000
    private val gson = Gson()
    private val client = OkHttpClient()

    /**
     * Reads a network resource.
     * If clazz is String, then the network response is returned as a string.
     * If class is not a String, then the network response is parsed as JSON and returned as object.
     * @throws NetworkException if the network resource is not available after 5 retires.
     * @throws GithubRateLimitExceededException if the GitHub-API rate limit is exceeded.
     */
    @MainThread
    suspend fun <T : Any> consumeAsync(url: String, clazz: KClass<T>): Deferred<T> {
        return withContext(Dispatchers.IO) {
            async {
                try {
                    consume(url, clazz)
                } catch (e: IOException) {
                    throw NetworkException("Fail to consume '$url'.", e)
                } catch (e: IllegalArgumentException) {
                    // for java.lang.IllegalArgumentException: port out of range:-1
                    throw NetworkException("Fail to consume '$url'.", e)
                } catch (e: SocketTimeoutException) {
                    // for java.net.SocketTimeoutException: timeout
                    throw NetworkException("Fail to consume '$url'.", e)
                }
            }
        }
    }

    @WorkerThread
    private fun <T : Any> consume(url: String, clazz: KClass<T>): T {
        require(url.startsWith("https://"))
        TrafficStats.setThreadStatsTag(threadId)

        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request)
            .execute()
            .use { response ->
                if (url.startsWith(GITHUB_URL) && response.code == 403) {
                    throw GithubRateLimitExceededException(Exception("response code is ${response.code}"))
                }

                val body = requireNotNull(response.body)

                if (clazz == String::class) {
                    @Suppress("UNCHECKED_CAST")
                    return body.string() as T
                }
                body.byteStream().buffered().reader().use { reader ->
                    return gson.fromJson(reader, clazz.java)
                }
            }
    }

    companion object {
        const val GITHUB_URL = "https://api.github.com"
    }
}