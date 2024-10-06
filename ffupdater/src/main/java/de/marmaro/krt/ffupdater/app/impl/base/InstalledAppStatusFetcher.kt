package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.exception.UnrecovableAppFetchException
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.network.LatestVersionCache
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.utils.MeasureExecutionTime

@Keep
interface InstalledAppStatusFetcher : InstalledVersionFetcher, LatestVersionFetcher, VersionDisplay {

    suspend fun isInstalledAppOutdated(context: Context, available: LatestVersion): Boolean {
        val installedVersion = getInstalledVersion(context.packageManager) ?: return true
        if (installedVersion.versionText == available.version.versionText && installedVersion.buildDate != null && available.version.buildDate != null) {
            return available.version.buildDate.isAfter(installedVersion.buildDate)
        }
        return VersionCompareHelper.isAvailableVersionHigher(
            installedVersion.versionText, available.version.versionText
        )
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
            throw NetworkException("Unable to fetch the latest update for ${app.name}.", e)
        } catch (e: DisplayableException) {
            throw DisplayableException("Unable to fetch the latest update for ${app.name}.", e)
        } catch (e: RuntimeException) {
            throw UnrecovableAppFetchException("Unable to fetch the latest update for ${app.name}.", e)
        }
    }

    private suspend fun convertToInstalledAppStatus(
        context: Context,
        latestVersion: LatestVersion,
    ): InstalledAppStatus {
        return InstalledAppStatus(
            app = app,
            latestVersion = latestVersion,
            isUpdateAvailable = isInstalledAppOutdated(context.applicationContext, latestVersion),
            displayVersion = getDisplayAvailableVersion(context.applicationContext, latestVersion)
        )
    }

    suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        InstalledAppsCache.updateCache(context.applicationContext)
    }
}