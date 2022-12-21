package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.ABI.*
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.InvalidApiResponseException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.PackageManagerUtil
import java.io.File


abstract class AppBase {
    abstract val app: App
    abstract val codeName: String
    abstract val packageName: String
    abstract val title: Int
    abstract val description: Int
    open val installationWarning: Int? = null
    abstract val downloadSource: String
    abstract val icon: Int
    abstract val minApiLevel: Int
    abstract val supportedAbis: List<ABI>
    abstract val signatureHash: String
    open val installableByUser: Boolean = true
    abstract val projectPage: String
    open val eolReason: Int? = null
    abstract val displayCategory: DisplayCategory
    open val fileNameInZipArchive: String? = null

    // TODO this must be executed on Dispatchers.IO
    @AnyThread
    fun isInstalled(context: Context): InstallationStatus {
        return if (PackageManagerUtil(context.packageManager).isAppInstalled(packageName)) {
            if (FingerprintValidator(context.packageManager).checkInstalledApp(this).isValid) {
                InstallationStatus.INSTALLED
            } else {
                InstallationStatus.INSTALLED_WITH_DIFFERENT_FINGERPRINT
            }
        } else {
            InstallationStatus.NOT_INSTALLED
        }
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

    fun isDownloadAnApkFile() = (fileNameInZipArchive == null)

    @AnyThread
    open fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
        // make sure that the main application uses the correct information about "update available status"
        val newMetadata = AppUpdateStatus(
            latestUpdate = available.latestUpdate,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, available.latestUpdate),
            displayVersion = getDisplayAvailableVersion(context, available.latestUpdate)
        )
        app.metadataCache.updateMetadataCache(context, newMetadata)
    }

    @Throws(NetworkException::class)
    suspend fun findAppUpdateStatus(context: Context): AppUpdateStatus {
        val available = try {
            findLatestUpdate(context)
        } catch (e: ApiRateLimitExceededException) {
            throw ApiRateLimitExceededException("Can't find latest update for $codeName.", e)
        } catch (e: InvalidApiResponseException) {
            throw InvalidApiResponseException("Can't find latest update for $codeName.", e)
        } catch (e: NetworkException) {
            throw NetworkException("Can't find latest update for $codeName.", e)
        } catch (e: Exception) {
            throw Exception("Can't find latest update for $codeName.", e)
        }
        return AppUpdateStatus(
            latestUpdate = available,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, available),
            displayVersion = getDisplayAvailableVersion(context, available)
        )
    }

    fun getMetadataCache(context: Context): AppUpdateStatus? {
        return try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getString("${CACHE_KEY_PREFIX}__${packageName}", null)
                ?.let { gson.fromJson(it, AppUpdateStatus::class.java) }
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    private fun updateMetadataCache(context: Context, appAppUpdateStatus: AppUpdateStatus): AppUpdateStatus {
        val jsonString = gson.toJson(appAppUpdateStatus)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
            .putString("${CACHE_KEY_PREFIX}__${packageName}", jsonString)
            .apply()
        return appAppUpdateStatus
    }

    @MainThread
    @Throws(NetworkException::class)
    internal abstract suspend fun findLatestUpdate(context: Context): LatestUpdate

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
        const val CACHE_TIME = 10 * MINUTE_IN_MILLIS
        const val CACHE_KEY_PREFIX = "cached_update_check_result__"
        val ALL_ABIS = listOf(ARM64_V8A, ARMEABI_V7A, ARMEABI, X86_64, X86, MIPS, MIPS64)
        val ARM32_ARM64_X86_X64 = listOf(ARM64_V8A, ARMEABI_V7A, X86_64, X86)
        val ARM32_ARM64 = listOf(ARM64_V8A, ARMEABI_V7A)
        val ARM32_X86 = listOf(ARMEABI_V7A, X86)
        val gson = Gson()
    }
}