package de.marmaro.krt.ffupdater.network

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Keep
object LatestVersionCache {
    private val dataCache: ConcurrentHashMap<App, LatestVersion> = ConcurrentHashMap()
    private val cacheAge: ConcurrentHashMap<App, LocalDateTime> = ConcurrentHashMap()

    private val RECENT_CACHE_THRESHOLD = Duration.ofHours(1)
    private val OLD_CACHE_THRESHOLD = Duration.ofDays(2)

    fun cache(app: App, latestVersion: LatestVersion) {
        dataCache[app] = latestVersion
        cacheAge[app] = LocalDateTime.now()
    }

    fun getRecent(app: App): LatestVersion? {
        return getCached(app, RECENT_CACHE_THRESHOLD)
    }

    fun getOld(app: App): LatestVersion? {
        return getCached(app, OLD_CACHE_THRESHOLD)
    }

    fun clear() {
        dataCache.clear()
        cacheAge.clear()
    }

    private fun getCached(app: App, threshold: Duration): LatestVersion? {
        val timestamp = cacheAge[app] ?: return null
        val cached = dataCache[app] ?: return null
        val age = Duration.between(timestamp, LocalDateTime.now())
        if (age > threshold) {
            return null
        }
        return cached
    }
}