package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.network.LatestVersionCache
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.utils.MeasureExecutionTime

@Keep
interface InstalledAppStatusFetcher : InstalledVersionFetcher, LatestVersionFetcher, VersionDisplay {

    suspend fun isInstalledAppOutdated(context: Context, available: LatestVersion): Boolean {
        val installedVersion = getInstalledVersion(context.packageManager) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    suspend fun findStatusOrUseRecentCache(context: Context): InstalledAppStatus {
        return findStatusAndCacheIt(context, LatestVersionCache.getRecent(app))
    }

    suspend fun findStatusOrUseOldCache(context: Context): InstalledAppStatus {
        return findStatusAndCacheIt(context, LatestVersionCache.getOld(app))
    }

    private suspend fun findStatusAndCacheIt(
        context: Context,
        cachedLatestVersion: LatestVersion?,
    ): InstalledAppStatus {
        if (cachedLatestVersion != null) {
            return convertToInstalledAppStatus(context, cachedLatestVersion)
        }

        try {
            Log.d(LOG_TAG, "InstalledAppStatusFetcher: Search for latest ${app.name} update.")
            val (latestVersion, duration) = MeasureExecutionTime.measureMs {
                fetchLatestUpdate(context.applicationContext)
            }
            LatestVersionCache.cache(app, latestVersion)
            Log.i(LOG_TAG, "InstalledAppStatusFetcher: Found ${app.name} ${latestVersion.version} (${duration}ms).")
            return convertToInstalledAppStatus(context, latestVersion)
        } catch (e: NetworkException) {
            Log.d(LOG_TAG, "InstalledAppStatusFetcher: Can't find latest update for ${app.name}.", e)
            throw NetworkException("can't find latest update for ${app.name}.", e)
        } catch (e: DisplayableException) {
            Log.d(LOG_TAG, "InstalledAppStatusFetcher: Can't find latest update for ${app.name}.", e)
            throw DisplayableException("can't find latest update for ${app.name}.", e)
        } catch (e: IllegalStateException) {
            Log.d(LOG_TAG, "InstalledAppStatusFetcher: Can't find latest update for ${app.name}.", e)
            throw IllegalStateException("can't find latest update for ${app.name}.", e)
        }
    }

    private suspend fun convertToInstalledAppStatus(
        context: Context,
        latestVersion: LatestVersion,
    ): InstalledAppStatus {
        return InstalledAppStatus(app = app, latestVersion = latestVersion,
                isUpdateAvailable = isInstalledAppOutdated(context.applicationContext, latestVersion),
                displayVersion = getDisplayAvailableVersion(context.applicationContext, latestVersion))
    }

    suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        InstalledAppsCache.updateCache(context.applicationContext)
    }
}