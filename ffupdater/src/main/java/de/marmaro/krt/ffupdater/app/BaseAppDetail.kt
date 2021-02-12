package de.marmaro.krt.ffupdater.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class BaseAppDetail : AppDetail {
    override fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    protected fun getInstalledVersionFromPackageManager(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    protected fun getInstalledVersionFromSharedPreferences(context: Context, key: String): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(key, null)
    }

    protected fun setInstalledVersionInSharedPreferences(context: Context, key: String, value: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(key, value).apply()
    }
    
    protected abstract fun isUpdateAvailable(updateCheckSubResult: UpdateCheckSubResult): Boolean

    private var cache: Deferred<UpdateCheckSubResult>? = null
    private var cacheTimestamp: Long = 0
    private val mutex = Mutex()

    /**
     * 2min timeout
     */
    override suspend fun updateCheckAsync(context: Context,
                                          deviceEnvironment: DeviceEnvironment): UpdateCheckResult {
        mutex.withLock {
            val cacheAge = System.currentTimeMillis() - cacheTimestamp
            // cache is invalid, if it's: too old, not created or failed
            if (cache == null || cacheAge > CACHE_TIME) {
                cache = GlobalScope.async(Dispatchers.IO) {
                    updateCheck(context, deviceEnvironment)
                }
                cacheTimestamp = System.currentTimeMillis()
            }
        }
        val updateCheckResult = cache!!.await()
        return updateCheckResult.convertToUpdateCheckResult(isUpdateAvailable(updateCheckResult))
    }

    companion object {
        const val CACHE_TIME: Long = 10 * 60 * 1000 // 10 minutes
    }
}