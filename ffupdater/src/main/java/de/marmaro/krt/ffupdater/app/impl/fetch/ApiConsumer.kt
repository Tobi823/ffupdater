package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.reflect.KClass


/**
 * Consume a REST-API from the internet.
 */
object ApiConsumer {
    private const val THREAD_ID = 10000
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
    suspend fun <T : Any> consumeNetworkResource(url: String, clazz: KClass<T>): T {
        return withContext(Dispatchers.IO) {
            readNetworkResource(url, clazz)
        }
    }

    @WorkerThread
    private fun <T : Any> readNetworkResource(url: String, clazz: KClass<T>): T {
        require(url.startsWith("https://"))
        TrafficStats.setThreadStatsTag(THREAD_ID)
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = requireNotNull(response.body)
                if (clazz == String::class) {
                    @Suppress("UNCHECKED_CAST")
                    return body.string() as T
                }
                body.byteStream().buffered().reader().use { reader ->
                    return gson.fromJson(reader, clazz.java)
                }
            }
        } catch (e: FileNotFoundException) {
            if (url.startsWith("https://api.github.com")) {
                throw GithubRateLimitExceededException(e)
            } else {
                throw NetworkException("Fail to consume ${clazz.qualifiedName} from '$url'.", e)
            }
        } catch (e: IOException) {
            throw NetworkException("Fail to consume ${clazz.qualifiedName} from '$url'.", e)
        }
    }
}