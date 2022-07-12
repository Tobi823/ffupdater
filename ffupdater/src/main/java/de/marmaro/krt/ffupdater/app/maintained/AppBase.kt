package de.marmaro.krt.ffupdater.app.maintained

import android.content.Context
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.PackageManagerUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


abstract class AppBase {
    abstract val packageName: String
    abstract val displayTitle: Int
    abstract val displayDescription: Int
    open val displayWarning: Int? = null
    abstract val displayDownloadSource: Int
    abstract val displayIcon: Int
    abstract val minApiLevel: Int
    abstract val supportedAbis: List<ABI>
    abstract val signatureHash: String
    open val installableWithDefaultPermission: Boolean = true
    abstract val projectPage: Uri
    open val eolReason: Int? = null

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
    fun getDisplayAvailableVersion(context: Context, availableVersionResult: LatestUpdate): String {
        return context.getString(R.string.available_version, availableVersionResult.version)
    }

    fun isEol() = (eolReason != null)

    fun showAsInstallable() = installableWithDefaultPermission && !isEol()

    @AnyThread
    open fun appIsInstalled(context: Context, available: AppUpdateStatus) {
        // make sure that the main application uses the correct information about "update available status"
        updateCacheAfterInstallation(context, available)
    }

    suspend fun checkForUpdateAsync(context: Context): Deferred<AppUpdateStatus> {
        return withContext(Dispatchers.IO) {
            async {
                mutex.withLock {
                    getUpdateCache(context)
                        ?.takeIf { System.currentTimeMillis() - it.objectCreationTimestamp <= CACHE_TIME }
                        ?.let { return@withLock it }

                    findAppUpdateStatus(context)
                }
            }
        }
    }

    suspend fun checkForUpdateWithoutUsingCacheAsync(context: Context): Deferred<AppUpdateStatus> {
        return withContext(Dispatchers.IO) {
            async {
                mutex.withLock {
                    findAppUpdateStatus(context)
                }
            }
        }
    }

    private suspend fun findAppUpdateStatus(context: Context): AppUpdateStatus {
        val available = findLatestUpdate()
        return AppUpdateStatus(
            latestUpdate = available,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, available),
            displayVersion = getDisplayAvailableVersion(context, available)
        ).also { result -> setUpdateCache(context, result) }
    }

    fun getUpdateCache(context: Context): AppUpdateStatus? {
        return try {
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString("${CACHE_KEY_PREFIX}__${packageName}", null)
                ?.let { gson.fromJson(it, AppUpdateStatus::class.java) }
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    private fun setUpdateCache(context: Context, appAppUpdateStatus: AppUpdateStatus) {
        val jsonString = gson.toJson(appAppUpdateStatus)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString("${CACHE_KEY_PREFIX}__${packageName}", jsonString)
            .apply()
    }

    private fun updateCacheAfterInstallation(context: Context, available: AppUpdateStatus): AppUpdateStatus {
        return AppUpdateStatus(
            latestUpdate = available.latestUpdate,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, available.latestUpdate),
            displayVersion = getDisplayAvailableVersion(context, available.latestUpdate)
        ).also { result -> setUpdateCache(context, result) }
    }

    @MainThread
    internal abstract suspend fun findLatestUpdate(): LatestUpdate

    @MainThread
    open suspend fun isAvailableVersionEqualToArchive(
        context: Context,
        file: File,
        available: LatestUpdate
    ): Boolean {
        val archiveVersion = PackageManagerUtil(context.packageManager)
            .getPackageArchiveVersionNameOrNull(file.absolutePath) ?: return false
        return VersionCompareHelper.isAvailableVersionEqual(archiveVersion, available.version)
    }

    @AnyThread
    open fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate
    ): Boolean {
        val installedVersion = getInstalledVersion(context) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    companion object {
        const val CACHE_TIME = 600_000L // 10 minutes
        const val CACHE_KEY_PREFIX = "cached_update_check_result__"
        val gson = Gson()
        val ALL_ABIS = listOf(
            ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.ARMEABI, ABI.X86_64, ABI.X86, ABI.MIPS,
            ABI.MIPS64
        )
        val ARM_AND_X_ABIS = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)
        val ARM_ABIS = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A)

    }
}