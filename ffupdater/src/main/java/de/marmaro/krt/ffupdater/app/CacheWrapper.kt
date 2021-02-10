package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.Deferred

class CacheWrapper(private val delegate: AppDetail) : AppDetail by delegate {
    private var cache: Deferred<UpdateCheckResult>? = null
    private var cacheTimestamp: Long = 0

    override fun updateCheckAsync(context: Context, deviceEnvironment: DeviceEnvironment): Deferred<UpdateCheckResult> {
        val cacheAge = System.currentTimeMillis() - cacheTimestamp
        // cache is invalid, if it's: too old, not created or failed
        if (cache == null || cacheAge > CACHE_TIME || cache!!.isCancelled) {
            cache = delegate.updateCheckAsync(context, deviceEnvironment)
            cacheTimestamp = System.currentTimeMillis()
        }
        return cache!!
    }

    companion object {
        const val CACHE_TIME: Long = 10 * 60 * 1000 // 10 minutes
    }
}