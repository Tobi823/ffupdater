package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour

@Keep
interface InstalledAppStatusFetcher : InstalledVersionFetcher, LatestVersionFetcher, VersionDisplay {

    fun isInstalledAppOutdated(context: Context, available: LatestVersion): Boolean {
        val installedVersion = getInstalledVersion(context.packageManager) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    suspend fun findAppUpdateStatus(context: Context, cacheBehaviour: CacheBehaviour): InstalledAppStatus {
        val available = try {
            Log.d(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Search for latest ${app.name} update.")
            val time = System.currentTimeMillis()
            val available = fetchLatestUpdate(context.applicationContext, cacheBehaviour)
            val duration = System.currentTimeMillis() - time
            Log.i(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Found ${app.name} ${available.version} (${duration}ms).")
            available
        } catch (e: NetworkException) {
            Log.d(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Can't find latest update for ${app.name}.", e)
            throw NetworkException("can't find latest update for ${app.name}.", e)
        } catch (e: DisplayableException) {
            Log.d(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Can't find latest update for ${app.name}.", e)
            throw DisplayableException("can't find latest update for ${app.name}.", e)
        }
        return InstalledAppStatus(
            latestVersion = available,
            isUpdateAvailable = isInstalledAppOutdated(context.applicationContext, available),
            displayVersion = getDisplayAvailableVersion(context.applicationContext, available)
        )
    }

    suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        InstalledAppsCache.updateCache(context.applicationContext)
    }
}