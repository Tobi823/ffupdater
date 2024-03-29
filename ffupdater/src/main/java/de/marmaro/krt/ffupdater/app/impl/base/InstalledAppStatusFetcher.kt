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
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.utils.MeasureExecutionTime
import java.lang.IllegalStateException

@Keep
interface InstalledAppStatusFetcher : InstalledVersionFetcher, LatestVersionFetcher, VersionDisplay {

    suspend fun isInstalledAppOutdated(context: Context, available: LatestVersion): Boolean {
        val installedVersion = getInstalledVersion(context.packageManager) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    suspend fun findInstalledAppStatus(context: Context, cacheBehaviour: CacheBehaviour): InstalledAppStatus {
        val available = try {
            Log.d(LOG_TAG, "InstalledAppStatusFetcher: Search for latest ${app.name} update.")
            val (result, duration) = MeasureExecutionTime.measureMs {
                fetchLatestUpdate(context.applicationContext, cacheBehaviour)
            }
            Log.i(LOG_TAG, "InstalledAppStatusFetcher: Found ${app.name} ${result.version} (${duration}ms).")
            result
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
        return InstalledAppStatus(
            app = app,
            latestVersion = available,
            isUpdateAvailable = isInstalledAppOutdated(context.applicationContext, available),
            displayVersion = getDisplayAvailableVersion(context.applicationContext, available)
        )
    }

    suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        InstalledAppsCache.updateCache(context.applicationContext)
    }
}