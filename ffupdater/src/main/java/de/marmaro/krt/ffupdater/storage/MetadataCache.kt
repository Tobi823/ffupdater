package de.marmaro.krt.ffupdater.storage

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.JsonSyntaxException
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

class MetadataCache(private val app: App) {
    private val preferenceKey = "${CACHE_KEY_PREFIX}__${app.impl.packageName}"
    private val mutex = Mutex()

    suspend fun getCachedOrFetchIfOutdated(context: Context): AppUpdateStatus {
        getCachedOrNullIfOutdated(context)
            ?.let { return it }

        mutex.withLock {
            // abort with the cache was updated when waiting for the mutx
            getCachedOrNullIfOutdated(context)
                ?.let { return it }

            val appUpdateStatus = try {
                app.impl.findAppUpdateStatus(context)
            } catch (e: ApiRateLimitExceededException) {
                throw ApiRateLimitExceededException("Can't find latest update for ${app.name}.", e)
            } catch (e: InvalidApiResponseException) {
                throw InvalidApiResponseException("Can't find latest update for ${app.name}.", e)
            } catch (e: NetworkException) {
                throw NetworkException("Can't find latest update for ${app.name}.", e)
            } catch (e: Exception) {
                throw Exception("Can't find latest update for ${app.name}.", e)
            }

            updateMetadataCache(context, appUpdateStatus)
            return appUpdateStatus
        }
    }

    fun getCachedOrNullIfOutdated(context: Context): AppUpdateStatus? {
        return getCached(context)
            ?.takeIf { System.currentTimeMillis() - it.objectCreationTimestamp <= CACHE_TIME.toMillis() }
    }

    fun getCachedOrExceptionIfOutdated(context: Context): AppUpdateStatus {
        return getCached(context) ?: throw IllegalArgumentException("cache is outdated")
    }

    fun getCached(context: Context): AppUpdateStatus? {
        return try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getString(preferenceKey, null)
                ?.let { AppBase.gson.fromJson(it, AppUpdateStatus::class.java) }
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    @SuppressLint("ApplySharedPref")
    internal fun updateMetadataCache(context: Context, appAppUpdateStatus: AppUpdateStatus): AppUpdateStatus {
        val jsonString = AppBase.gson.toJson(appAppUpdateStatus)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
            .putString(preferenceKey, jsonString)
            .commit()
        return appAppUpdateStatus
    }


    @SuppressLint("ApplySharedPref")
    fun invalidateCache(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
            .putString(preferenceKey, null)
            .commit()
    }

    companion object {
        val CACHE_TIME: Duration = Duration.ofMinutes(10)
        const val CACHE_KEY_PREFIX = "cached_update_check_result__"
    }
}