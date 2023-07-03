package de.marmaro.krt.ffupdater.network.file

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

internal class ProgressInterceptorResponseBody(
    private val originalUrl: HttpUrl,
    private val responseBody: ResponseBody,
    private var processChannel: Channel<DownloadStatus>?,
) : ResponseBody() {

    override fun contentType() = responseBody.contentType()
    override fun contentLength() = responseBody.contentLength()
    override fun source() = trackTransmittedBytes(responseBody.source()).buffer()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun trackTransmittedBytes(source: Source): Source {
        Log.i(LOG_TAG, "Make network request: $originalUrl")
        if (processChannel?.isClosedForSend == true) {
            processChannel = null
        }

        // create a new object for tracking
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            var reportedPercentage = -1
            var reportedMB = -1L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                if (bytesRead != SOURCE_IS_EXHAUSTED) {
                    totalBytesRead += bytesRead
                }
                if (contentLength() > 0L) {
                    reportPercentage()
                } else {
                    reportMB()
                }
                return bytesRead
            }

            private fun reportPercentage() {
                val progress = (100 * totalBytesRead / contentLength()).toInt()
                if (progress != reportedPercentage) {
                    reportedPercentage = progress
                    processChannel?.trySend(
                        DownloadStatus(
                            progress,
                            toMB(totalBytesRead)
                        )
                    )
                }
            }

            private fun reportMB() {
                val totalMegabytesRead = toMB(totalBytesRead)
                if (totalMegabytesRead != reportedMB) {
                    reportedMB = totalMegabytesRead
                    processChannel?.trySend(DownloadStatus(null, totalMegabytesRead))
                }
            }

            override fun close() {
                source.close()
                super.close()
            }
        }
    }

    companion object {
        private const val LOG_TAG = "FileDownloader"
        private const val BYTES_IN_MB = 1_048_576
        private const val SOURCE_IS_EXHAUSTED = -1L
        private fun toMB(bytes: Long): Long {
            return bytes / BYTES_IN_MB
        }
    }
}