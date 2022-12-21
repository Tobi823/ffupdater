package de.marmaro.krt.ffupdater.network

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass


/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    private val gson = Gson()

    suspend fun consume(url: String, settings: NetworkSettingsHelper): String {
        val fileDownloader = FileDownloader(settings)
        return fileDownloader.downloadSmallFileAsync(url)
    }

    suspend fun <T : Any> consume(url: String, settings: NetworkSettingsHelper, clazz: KClass<T>): T {
        return withContext(Dispatchers.IO) {
            val stringResponse = consume(url, settings)
            gson.fromJson(stringResponse, clazz.java) // in IO thread because it could be a lot of work
        }
    }

    companion object {
        val INSTANCE = ApiConsumer()
    }
}