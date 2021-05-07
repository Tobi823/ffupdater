package de.marmaro.krt.ffupdater.app

import android.content.Context
import android.content.pm.PackageManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiNetworkException
import de.marmaro.krt.ffupdater.app.impl.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
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

    /**
     * @throws InvalidApiResponseException
     * @throws ApiNetworkException
     */
    protected abstract suspend fun updateCheckWithoutCaching(): AvailableVersionResult


    override suspend fun updateCheck(context: Context): UpdateCheckResult {
        //TODO Vielleicht UpdateCheckResult nicht exponieren, damit ich mich nicht auf was festlegen muss
        mutex.withLock {
            val cacheAge = System.currentTimeMillis() - cacheTimestamp
            // cache is invalid, if it's: too old, not created or failed
            if (cache == null || cacheAge > CACHE_TIME) {
                cache = GlobalScope.async(Dispatchers.IO) {
                    updateCheckWithoutCaching()
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
                isUpdateAvailable = !isInstalledVersionUpToDate(context, availableVersionResult),
                displayVersion = getDisplayAvailableVersion(context, availableVersionResult))
    }

    override suspend fun isCacheFileUpToDate(
            context: Context,
            file: File,
            availableVersionResult: AvailableVersionResult,
    ): Boolean {
        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                ?: return false
        return packageInfo.versionName == availableVersionResult.version
    }

    override suspend fun isInstalledVersionUpToDate(
            context: Context,
            availableVersionResult: AvailableVersionResult,
    ): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            return packageInfo.versionName == availableVersionResult.version
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun appInstallationCallback(context: Context) {}

    /**
     * Helper function for getting the correct ABI name (these names are app specific)
     */
    protected fun getStringForCurrentAbi(
            arm: String?,
            arm64: String?,
            x86: String?,
            x64: String?,
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
}