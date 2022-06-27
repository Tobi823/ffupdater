package de.marmaro.krt.ffupdater.app

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.download.PackageManagerUtil
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


abstract class BaseApp {
    abstract val packageName: String
    abstract val displayTitle: Int
    abstract val displayDescription: Int
    abstract val displayWarning: Int?
    abstract val displayDownloadSource: Int
    abstract val displayIcon: Int
    abstract val minApiLevel: Int
    abstract val supportedAbis: List<ABI>
    abstract val signatureHash: String

    // The installation does not require special actions like rooting the smartphone
    abstract val normalInstallation: Boolean

    private val mutex = Mutex()

    @AnyThread
    fun isInstalled(context: Context): Boolean {
        return PackageManagerUtil(context.packageManager).isAppInstalled(packageName) &&
                FingerprintValidator(context.packageManager).checkInstalledApp(this).isValid
    }

    @AnyThread
    open fun getInstalledVersion(context: Context): String? {
        return PackageManagerUtil(context.packageManager).getInstalledAppVersionName(packageName)
    }

    @AnyThread
    fun getDisplayInstalledVersion(context: Context): String {
        return context.getString(R.string.installed_version, getInstalledVersion(context))
    }

    @AnyThread
    fun getDisplayAvailableVersion(context: Context, availableVersionResult: AvailableVersionResult): String {
        return context.getString(R.string.available_version, availableVersionResult.version)
    }

    @AnyThread
    open fun appIsInstalled(context: Context, available: AvailableVersionResult) {
    }

    suspend fun checkForUpdateAsync(context: Context): Deferred<UpdateCheckResult> {
        return withContext(Dispatchers.IO) {
            async {
                // - use mutex lock to prevent multiple simultaneously update check for a single app
                mutex.withLock {
                    checkForUpdateAndCacheIt(context, true)
                }
            }
        }
    }

    suspend fun checkForUpdateWithoutCacheAsync(context: Context): Deferred<UpdateCheckResult> {
        return withContext(Dispatchers.IO) {
            async {
                // - use mutex lock to prevent multiple simultaneously update check for a single app
                mutex.withLock {
                    checkForUpdateAndCacheIt(context, false)
                }
            }
        }
    }

    private suspend fun checkForUpdateAndCacheIt(context: Context, useCache: Boolean): UpdateCheckResult {
        val cacheKey = "cached_update_check_result__${packageName}"
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        try {
            if (useCache) {
                preferences.getString(cacheKey, null)
                    ?.let { gson.fromJson(it, UpdateCheckResult::class.java) }
                    ?.takeIf { System.currentTimeMillis() - it.timestamp <= CACHE_TIME }
                    ?.let { return it }
            }
        } catch (e: JsonSyntaxException) {
            // just ignore the invalid cache
        }

        val available = checkForUpdate()
        val result = UpdateCheckResult(
            availableResult = available,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, available),
            displayVersion = getDisplayAvailableVersion(context, available)
        )

        preferences.edit().putString(cacheKey, gson.toJson(result)).apply()
        return result
    }

    @MainThread
    protected abstract suspend fun checkForUpdate(): AvailableVersionResult

    @MainThread
    open suspend fun isAvailableVersionEqualToArchive(
        context: Context,
        file: File,
        available: AvailableVersionResult
    ): Boolean {
        val archiveVersion = PackageManagerUtil(context.packageManager)
            .getPackageArchiveVersionNameOrNull(file.absolutePath) ?: return false
        return VersionCompareHelper.isAvailableVersionEqual(archiveVersion, available.version)
    }

    @AnyThread
    open fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: AvailableVersionResult
    ): Boolean {
        val installedVersion = getInstalledVersion(context) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    companion object {
        const val CACHE_TIME = 600_000L // 10 minutes
        val gson = Gson()
    }
}