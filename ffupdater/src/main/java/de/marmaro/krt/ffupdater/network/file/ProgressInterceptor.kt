package de.marmaro.krt.ffupdater.network.file

import kotlinx.coroutines.channels.Channel
import okhttp3.Interceptor
import okhttp3.Response

internal class ProgressInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.proceed(chain.request())
        val responseBody = requireNotNull(original.body) { "original.body is null! Maybe called by cache?" }
        val processChannel = original.request.tag() as Channel<DownloadStatus>?
        return original.newBuilder()
            // ignore must-revalidate for cache-control
            // override max-age because I want to keep cache entries for USE_EVEN_VERY_OLD_CACHE up to 2 days
            .header("cache-control", "max-age=172800")
            // the age should be determined by OkHttp-Received-Millis and not by the server
            .removeHeader("last-modified")
            .removeHeader("date")
            .removeHeader("age")
            .body(ProgressInterceptorResponseBody(original.request.url, responseBody, processChannel))
            .build()
    }
}