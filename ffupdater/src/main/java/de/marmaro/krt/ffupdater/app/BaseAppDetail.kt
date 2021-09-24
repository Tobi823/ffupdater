package de.marmaro.krt.ffupdater.app

import android.content.Context
import android.content.pm.PackageManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiConsumerException
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

abstract class BaseAppDetail : AppDetail {
    private var cache: CachedAvailableVersionResult? = null
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

    /**
     * @throws InvalidApiResponseException
     * @throws ApiConsumerException
     */
    protected abstract fun updateCheckWithoutCaching(): AvailableVersionResult

    override suspend fun updateCheck(context: Context): UpdateCheckResult {
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

        val availableVersionResult = cache!!.availableVersionResult
        return UpdateCheckResult(
                availableResult = availableVersionResult,
                isUpdateAvailable = !isInstalledVersionUpToDate(context, availableVersionResult),
                displayVersion = getDisplayAvailableVersion(context, availableVersionResult))
    }

    override suspend fun isCacheFileUpToDate(
            context: Context,
            file: File,
            available: AvailableVersionResult,
    ): Boolean {
        val pm = context.packageManager
        val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return false
        return packageInfo.versionName == available.version
    }

    override suspend fun isInstalledVersionUpToDate(
            context: Context,
            available: AvailableVersionResult,
    ): Boolean {
        return getInstalledVersion(context) == available.version
    }

    override fun appInstallationCallback(context: Context, available: AvailableVersionResult) {}

    /**
     * Helper function for getting the correct ABI name (these names are app specific)
     */
    protected fun getStringForCurrentAbi(
            arm: String? = null,
            arm64: String? = null,
            x86: String? = null,
            x64: String? = null,
    ): String {
        return DeviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> arm64
                ABI.ARMEABI_V7A -> arm
                ABI.X86 -> x86
                ABI.X86_64 -> x64
                ABI.ARMEABI, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
    }

    companion object {
        const val CACHE_TIME: Long = 10 * 60 * 1000 // 10 minutes
    }

    data class CachedAvailableVersionResult(
        val availableVersionResult: AvailableVersionResult,
        val cacheTimestamp: Long
    )
}