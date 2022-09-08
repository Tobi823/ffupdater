package de.marmaro.krt.ffupdater.network

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KMutableProperty0

class FileDownloader(networkSettingsHelper: NetworkSettingsHelper) {
    private val trafficStatsThreadId = 10001

    var onProgress: (progressInPercent: Int?, totalMB: Long) -> Unit = @WorkerThread { _, _ -> }

    // fallback to wait for the download when the activity was recreated
    var currentDownload: Deferred<Any>? = null

    private val onlyTrustSystemCAs = !networkSettingsHelper.areUserCAsTrusted

    @MainThread
    @Throws(NetworkException::class)
    suspend fun downloadFileAsync(url: String, file: File): Deferred<Any> {
        return withContext(Dispatchers.IO) {
            currentDownload = async {
                try {
                    lastChange = System.currentTimeMillis()
                    numberOfRunningDownloads.incrementAndGet()
                    downloadFileInternal(url, file)
                } catch (e: IOException) {
                    throw NetworkException("Download of $url failed.", e)
                } catch (e: NetworkException) {
                    throw NetworkException("Download of $url failed.", e)
                } finally {
                    lastChange = System.currentTimeMillis()
                    numberOfRunningDownloads.decrementAndGet()
                }
            }
            currentDownload!!
        }
    }

    @WorkerThread
    private suspend fun downloadFileInternal(url: String, file: File) {
        val okhttpCallResponse = callUrl(url)
        okhttpCallResponse.use { response ->
            TrafficStats.setThreadStatsTag(trafficStatsThreadId)
            val body = response.body
            if (!response.isSuccessful) {
                throw NetworkException("Response is unsuccessful. HTTP code: '${response.code}'.")
            }
            if (body == null) {
                throw NetworkException("Response is unsuccessful. Body is null.")
            }
            if (file.exists()) {
                file.delete()
            }
            file.outputStream().buffered().use { fileWriter ->
                body.byteStream().buffered().use { responseReader ->
                    // this method blocks until download is finished
                    responseReader.copyTo(fileWriter)
                }
            }
        }
    }

    private suspend fun callUrl(url: String): Response {
        require(url.startsWith("https://"))
        TrafficStats.setThreadStatsTag(trafficStatsThreadId)
        val client = if (onlyTrustSystemCAs) {
            createClientTrustingOnlySystemCAs()
        } else {
            createClientAcceptingSystemAndUserCAs()
        }
        val request = Request.Builder()
            .url(url)
            .build()
        return client.newCall(request)
            .await()
    }

    private fun createClientTrustingOnlySystemCAs(): OkHttpClient {
        return OkHttpClient.Builder()
            .addNetworkInterceptor { chain: Interceptor.Chain ->
                val original = chain.proceed(chain.request())
                val body = requireNotNull(original.body)
                original.newBuilder()
                    .body(ProgressResponseBody(body, this::onProgress))
                    .build()
            }
            .build()
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
            .addNetworkInterceptor { chain: Interceptor.Chain ->
                val original = chain.proceed(chain.request())
                val body = requireNotNull(original.body)
                original.newBuilder()
                    .body(ProgressResponseBody(body, this::onProgress))
                    .build()
            }
            .build()
    }

    // simple communication between WorkManager and the InstallActivity to prevent duplicated downloads
    // persistence/consistence is not very important -> global available variables are ok
    companion object {
        private var numberOfRunningDownloads = AtomicInteger(0)
        private var lastChange = System.currentTimeMillis()
        fun areDownloadsCurrentlyRunning() = (numberOfRunningDownloads.get() != 0) &&
                ((System.currentTimeMillis() - lastChange) < 3600_000)
    }
}

internal class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private var onProgress: KMutableProperty0<(progressInPercent: Int?, totalMB: Long) -> Unit>
) : ResponseBody() {
    override fun contentType() = responseBody.contentType()
    override fun contentLength() = responseBody.contentLength()
    override fun source() = trackTransmittedBytes(responseBody.source()).buffer()

    private fun trackTransmittedBytes(source: Source): Source {
        return object : ForwardingSource(source) {
            private val sourceIsExhausted = -1L
            var totalBytesRead = 0L
            var totalProgress = -1
            var totalMB = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                if (bytesRead != sourceIsExhausted) {
                    totalBytesRead += bytesRead
                }
                if (contentLength() > 0L) {
                    val progress = (100 * totalBytesRead / contentLength()).toInt()
                    if (progress != totalProgress) {
                        totalProgress = progress
                        val totalMegabytesRead = totalBytesRead / 1_048_576
                        onProgress.get().invoke(progress, totalMegabytesRead)
                    }
                } else {
                    val totalMegabytesRead = totalBytesRead / 1_048_576
                    if (totalMegabytesRead != totalMB) {
                        totalMB = totalMegabytesRead
                        onProgress.get().invoke(null, totalMegabytesRead)
                    }
                }
                return bytesRead
            }
        }
    }
}
