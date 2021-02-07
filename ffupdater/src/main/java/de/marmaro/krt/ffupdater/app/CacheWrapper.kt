package de.marmaro.krt.ffupdater.app

import android.content.Context
import de.marmaro.krt.ffupdater.device.ABI
import kotlinx.coroutines.Deferred

class CacheWrapper(private val delegate: App) : App by delegate {
    private var cache: Deferred<UpdateCheckResult>? = null
    private var cacheTimestamp: Long = 0

    override fun updateCheckAsync(context: Context, abi: ABI): Deferred<UpdateCheckResult> {
        val cacheAge = System.currentTimeMillis() - cacheTimestamp
        // cache is invalid, if it's: too old, not created or failed
        if (cache == null || cacheAge > CACHE_TIME || cache!!.isCancelled) {
            cache = delegate.updateCheckAsync(context, abi)
            cacheTimestamp = System.currentTimeMillis()
        }
        return cache!!
    }

    companion object {
        const val CACHE_TIME: Long = 10 * 60 * 1000 // 10 minutes
    }
}