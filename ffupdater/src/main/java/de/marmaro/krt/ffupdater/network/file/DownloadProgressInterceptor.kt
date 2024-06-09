package de.marmaro.krt.ffupdater.network.file

import androidx.annotation.Keep
import kotlinx.coroutines.channels.Channel
import okhttp3.Interceptor
import okhttp3.Response

@Keep
class DownloadProgressInterceptor : Interceptor {

    @Throws(IllegalArgumentException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.proceed(chain.request())
        val responseBody = requireNotNull(original.body) { "original.body is null! Maybe called by cache?" }

        @Suppress("UNCHECKED_CAST")
        val processChannel = original.request.tag() as? Channel<DownloadStatus>
        return original.newBuilder()
            .body(ProgressInterceptorResponseBody(original.request.url, responseBody, processChannel))
            .build()
    }
}