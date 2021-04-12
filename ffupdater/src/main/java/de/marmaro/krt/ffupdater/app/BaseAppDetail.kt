package de.marmaro.krt.ffupdater.app

import android.content.Context
import android.content.pm.PackageManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

abstract class BaseAppDetail : AppDetail {
    private var cache: Deferred<AvailableVersionResult>? = null
    private var cacheTimestamp: Long = 0
    private val mutex = Mutex()

    override fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getDisplayInstalledVersion(context: Context): String {
        return context.getString(R.string.installed_version, getInstalledVersion(context))
    }

    override fun getInstalledVersion(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    protected open fun getDisplayAvailableVersion(
            context: Context,
            availableVersionResult: AvailableVersionResult,
    ): String {
        return context.getString(R.string.available_version, availableVersionResult.version)
    }

    protected abstract suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment)
            : AvailableVersionResult

    /**
     * 2min timeout
     */
    override suspend fun updateCheck(
            context: Context,
            deviceEnvironment: DeviceEnvironment,
    ): UpdateCheckResult {
        mutex.withLock {
            val cacheAge = System.currentTimeMillis() - cacheTimestamp
            // cache is invalid, if it's: too old, not created or failed
            if (cache == null || cacheAge > CACHE_TIME) {
                cache = GlobalScope.async(Dispatchers.IO) {
                    updateCheckWithoutCaching(deviceEnvironment)
                }
                cacheTimestamp = System.currentTimeMillis()
            }
        }

        val availableVersionResult: AvailableVersionResult
        withTimeout(Duration.ofMinutes(2).toMillis()) {
            availableVersionResult = cache!!.await()
        }
        return UpdateCheckResult(
                availableResult = availableVersionResult,
                isUpdateAvailable = areVersionsDifferent(getInstalledVersion(context), availableVersionResult),
                displayVersion = getDisplayAvailableVersion(context, availableVersionResult)
        )
    }

    override fun areVersionsDifferent(installedVersion: String?, available: AvailableVersionResult): Boolean {
        return installedVersion != available.version
    }

    companion object {
        const val CACHE_TIME: Long = 10 * 60 * 1000 // 10 minutes
    }
}