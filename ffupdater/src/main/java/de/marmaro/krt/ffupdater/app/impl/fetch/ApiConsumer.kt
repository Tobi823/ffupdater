package de.marmaro.krt.ffupdater.app.impl.fetch

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    fun <T> consume(url: URL, clazz: Class<T>): T {
        try {
            val connection = url.openConnection() as HttpsURLConnection
            connection.setRequestProperty("Accept-Encoding", GZIP)
            connection.connectTimeout = 10 * 1000 // 10 seconds
            connection.inputStream
                    .let { if (connection.contentEncoding == GZIP) GZIPInputStream(it) else it }
                    .let { InputStreamReader(it) }
                    .let { BufferedReader(it) }
                    .use { return Gson().fromJson(it, clazz) }
        } catch (e: IOException) {
            throw ApiConsumerNetworkException("can't consume API interface '$url'", e)
        }
    }

    companion object {
        private const val GZIP = "gzip"
    }

    class ApiConsumerNetworkException(message: String, throwable: Throwable) : Exception(message, throwable)
}