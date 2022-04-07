package de.marmaro.krt.ffupdater.download

import android.net.TrafficStats
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
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
import java.net.UnknownHostException
import kotlin.reflect.KMutableProperty0

class FileDownloader {
    private val trafficStatsThreadId = 10001
    var errorMessage: String? = null
        private set
    var onProgress: (progressInPercent: Int?, totalMB: Long) -> Unit = @WorkerThread { _, _ -> }

    // fallback to register for news for existing download
    var currentDownloadResult: Deferred<Boolean>? = null

    @MainThread
    suspend fun downloadFile(url: String, file: File): Boolean {
        return withContext(Dispatchers.IO) {
            val asyncValue = async {
                val value = downloadFileInternal(url, file)
                currentDownloadResult = null
                value
            }
            currentDownloadResult = asyncValue
            asyncValue
        }.await()
    }

    @WorkerThread
    private suspend fun downloadFileInternal(url: String, file: File): Boolean {
        require(url.startsWith("https://"))
        TrafficStats.setThreadStatsTag(trafficStatsThreadId)
        val client = createClient()
        val call = callUrl(client, url) ?: return false
        call.use { response ->
            TrafficStats.setThreadStatsTag(trafficStatsThreadId)
            val body = response.body
            if (!response.isSuccessful || body == null) {
                errorMessage = "HTTP code: ${response.code}"
                return false
            }
            file.outputStream().buffered().use { fileWriter ->
                body.byteStream().buffered().use { responseReader ->
                    // this method blocks until download is finished
                    responseReader.copyTo(fileWriter)
                    return true
                }
            }
        }
    }

    private suspend fun callUrl(client: OkHttpClient, url: String): Response? {
        val request = Request.Builder()
            .url(url)
            .build()
        return try {
            client.newCall(request)
                .await()
        } catch (e: UnknownHostException) {
            errorMessage = e.localizedMessage
            null
        }
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

// simple communication between WorkManager and the InstallActivity to prevent duplicated downloads
// persistence/consistence is not important -> global available variables are ok
class AppDownloadStatus {
    companion object {
        private var BACKGROUND_DOWNLOAD_STARTED: Long? = null
        private var FOREGROUND_DOWNLOAD_STARTED: Long? = null

        fun areDownloadsInBackgroundActive() =
            (System.currentTimeMillis() - (BACKGROUND_DOWNLOAD_STARTED ?: 0)) < 600_000L

        fun areDownloadsInForegroundActive() =
            (System.currentTimeMillis() - (FOREGROUND_DOWNLOAD_STARTED ?: 0)) < 600_000L

        fun backgroundDownloadIsStarted() {
            BACKGROUND_DOWNLOAD_STARTED = System.currentTimeMillis()
        }

        fun backgroundDownloadIsFinished() {
            BACKGROUND_DOWNLOAD_STARTED = null
        }

        fun foregroundDownloadIsStarted() {
            FOREGROUND_DOWNLOAD_STARTED = System.currentTimeMillis()
        }

        fun foregroundDownloadIsFinished() {
            FOREGROUND_DOWNLOAD_STARTED = null
        }
    }
}