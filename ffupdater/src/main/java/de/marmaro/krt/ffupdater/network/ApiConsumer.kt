package de.marmaro.krt.ffupdater.network

import android.content.Context
import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KClass


/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    private val threadId = 10000
    private val gson = Gson()

    /**
     * Reads a network resource.
     * If clazz is String, then the network response is returned as a string.
     * If class is not a String, then the network response is parsed as JSON and returned as object.
     * @throws NetworkException if the network resource is not available after 5 retires.
     * @throws ApiRateLimitExceededException if the GitHub-API rate limit is exceeded.
     */
    @MainThread
    @Throws(NetworkException::class)
    suspend fun <T : Any> consumeAsync(url: String, clazz: KClass<T>, context: Context): Deferred<T> {
        return withContext(Dispatchers.IO) {
            async {
                try {
                    consume(url, clazz, context)
                } catch (e: IOException) {
                    throw NetworkException("Fail to consume '$url'.", e)
                } catch (e: IllegalArgumentException) {
                    // for java.lang.IllegalArgumentException: port out of range:-1
                    throw NetworkException("Fail to consume '$url'.", e)
                } catch (e: SocketTimeoutException) {
                    // for java.net.SocketTimeoutException: timeout
                    throw NetworkException("Fail to consume '$url'.", e)
                } catch (e: ApiRateLimitExceededException) {
                    throw NetworkException("Fail to consume '$url'.", e)
                }
            }
        }
    }

    @WorkerThread
    @Throws(ApiRateLimitExceededException::class)
    private fun <T : Any> consume(url: String, clazz: KClass<T>, context: Context): T {
        require(url.startsWith("https://"))
        TrafficStats.setThreadStatsTag(threadId)

        val request = Request.Builder()
            .url(url)
            .build()

        val client = if (NetworkSettingsHelper(context).areUserCAsTrusted) {
            createClientAcceptingSystemAndUserCAs()
        } else {
            createClientTrustingOnlySystemCAs()
        }
        client.newCall(request)
            .execute()
            .use { response ->
                if (url.startsWith(GITHUB_URL) && response.code == 403) {
                    throw ApiRateLimitExceededException(
                        "API rate limit for GitHub is exceeded.",
                        Exception("response code is ${response.code}")
                    )
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

    private fun createClientTrustingOnlySystemCAs(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    private fun createClientAcceptingSystemAndUserCAs(): OkHttpClient {
        // https://developer.android.com/reference/java/security/KeyStore
        val systemAndUserCAStore = KeyStore.getInstance("AndroidCAStore")
        systemAndUserCAStore.load(null)

        // https://stackoverflow.com/a/24401795
        // https://github.com/bitfireAT/davx5-ose/blob/0e93a47d6d7277d3a18e31c6528f578c467a56ea/app/src/main/java/at/bitfire/davdroid/HttpClient.kt
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(systemAndUserCAStore)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(systemAndUserCAStore, "keystore_pass".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())

        val trustManager = trustManagerFactory.trustManagers.first() as X509TrustManager

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    companion object {
        const val GITHUB_URL = "https://api.github.com"
        val INSTANCE = ApiConsumer()
    }
}