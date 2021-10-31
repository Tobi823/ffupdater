package de.marmaro.krt.ffupdater.app.impl.fetch

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
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
    private fun <T : Any> readNetworkResource(urlString: String, clazz: KClass<T>): T {
        TrafficStats.setThreadStatsTag(THREAD_ID)
        val reader = try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.setRequestProperty("Accept-Encoding", GZIP)
            if (connection.contentEncoding == GZIP) {
                BufferedReader(InputStreamReader(GZIPInputStream(connection.inputStream)))
            } else {
                BufferedReader(InputStreamReader(connection.inputStream))
            }
        } catch (e: FileNotFoundException) {
            if (urlString.startsWith("https://api.github.com")) {
                throw GithubRateLimitExceededException(e)
            } else {
                throw NetworkException("Fail to consume ${clazz.qualifiedName} from '$urlString'.", e)
            }
        } catch (e: IOException) {
            throw NetworkException("Fail to consume ${clazz.qualifiedName} from '$urlString'.", e)
        }

        if (clazz == String::class) {
            @Suppress("UNCHECKED_CAST")
            return reader.use { it.readText() as T }
        }
        return reader.use { gson.fromJson(it, clazz.java) }
    }
}