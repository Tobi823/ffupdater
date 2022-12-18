package de.marmaro.krt.ffupdater.network

import androidx.annotation.MainThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass


/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    private val threadId = 10000
    private val gson = Gson()

    @MainThread
    @Throws(NetworkException::class)
    suspend fun <T : Any> consumeAsync(
        url: String,
        settings: NetworkSettingsHelper,
        clazz: KClass<T>
    ): Deferred<T> {
        return withContext(Dispatchers.IO) {
            async {
                val fileDownloader = FileDownloader(settings)
                val stringResponse = fileDownloader.downloadSmallFileAsync(url).await()
                if (clazz == String::class) {
                    @Suppress("UNCHECKED_CAST")
                    stringResponse as T
                } else {
                    gson.fromJson(stringResponse, clazz.java)
                }
            }
        }
    }

    companion object {
        val INSTANCE = ApiConsumer()
    }
}