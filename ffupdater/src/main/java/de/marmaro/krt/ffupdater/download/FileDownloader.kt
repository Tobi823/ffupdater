package de.marmaro.krt.ffupdater.download

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KMutableProperty0

class FileDownloader {
    private val trafficStatsThreadId = 10001

    var onProgress: (progressInPercent: Int?, totalMB: Long) -> Unit = @WorkerThread { _, _ -> }

    // fallback to wait for the download when the activity was recreated
    var currentDownload: Deferred<Any>? = null

    @MainThread
    @Throws(NetworkException::class)
    suspend fun downloadFileAsync(url: String, file: File): Deferred<Any> {
        return withContext(Dispatchers.IO) {
            currentDownload = async {
                try {
                    numberOfRunningDownloads.incrementAndGet()
                    downloadFileInternal(url, file)
                } catch (e: IOException) {
                    throw NetworkException("File download failed.", e)
                } catch (e: NetworkException) {
                    throw NetworkException("File download failed.", e)
                } finally {
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
        val client = createClient()
        val request = Request.Builder()
            .url(url)
            .build()
        return client.newCall(request)
            .await()
    }

    private fun createClient(): OkHttpClient {
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

    // simple communication between WorkManager and the InstallActivity to prevent duplicated downloads
    // persistence/consistence is not very important -> global available variables are ok
    companion object {
        private var numberOfRunningDownloads = AtomicInteger(0)
        fun areDownloadsCurrentlyRunning() = numberOfRunningDownloads.get() != 0
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
