package de.marmaro.krt.ffupdater.network

import androidx.annotation.MainThread
import com.google.gson.Gson
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import kotlin.reflect.KClass


/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    private val gson = Gson()

    @MainThread
    @Throws(NetworkException::class)
    suspend fun consume(url: String, settings: NetworkSettingsHelper): String {
        val fileDownloader = FileDownloader(settings)
        return fileDownloader.downloadSmallFileAsync(url)
    }

    @MainThread
    @Throws(NetworkException::class)
    suspend fun <T : Any> consume(url: String, settings: NetworkSettingsHelper, clazz: KClass<T>): T {
        val stringResponse = consume(url, settings)
        return gson.fromJson(stringResponse, clazz.java)
    }

    companion object {
        val INSTANCE = ApiConsumer()
    }
}