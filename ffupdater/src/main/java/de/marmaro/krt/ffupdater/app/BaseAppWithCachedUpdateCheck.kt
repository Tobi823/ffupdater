package de.marmaro.krt.ffupdater.app

import android.content.Context
import androidx.annotation.MainThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class BaseAppWithCachedUpdateCheck : BaseApp {
    private var cache: CachedAvailableVersionResult? = null
    private val mutex = Mutex()

    @MainThread
    protected abstract suspend fun updateCheckWithoutCaching(): AvailableVersionResult

    override suspend fun updateCheckAsync(context: Context): Deferred<UpdateCheckResult> {
        return withContext(Dispatchers.IO) {
            async {
                updateCheck(context)
            }
        }
    }

    private suspend fun updateCheck(context: Context): UpdateCheckResult {
        // - use mutex lock to prevent multiple simultaneously update check for a single app
        // - it's useless to start a new update check for an app when a different update check
        // for the same app is already running
        mutex.withLock {
            val cacheAge = System.currentTimeMillis() - (cache?.cacheTimestamp ?: 0)
            if (cache == null || cacheAge > CACHE_TIME) {
                val availableVersionResult = updateCheckWithoutCaching()
                val time = System.currentTimeMillis()
                cache = CachedAvailableVersionResult(availableVersionResult, time)
            }
        }

        val availableVersionResult = checkNotNull(cache).availableVersionResult
        return UpdateCheckResult(
            availableResult = availableVersionResult,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, availableVersionResult),
            displayVersion = getDisplayAvailableVersion(context, availableVersionResult)
        )
    }

    companion object {
        const val CACHE_TIME = 600_000L // 10 minutes
    }

    data class CachedAvailableVersionResult(
        val availableVersionResult: AvailableVersionResult,
        val cacheTimestamp: Long
    )
}