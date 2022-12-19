package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.marmaro.krt.ffupdater.R
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream


abstract class AppBase {
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

    private val mutex = Mutex()

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
    open fun appIsInstalled(context: Context, available: AppUpdateStatus) {
        // make sure that the main application uses the correct information about "update available status"
        updateCacheAfterInstallation(context, available)
    }

    @Throws(NetworkException::class)
    suspend fun checkForUpdateAsync(context: Context): Deferred<AppUpdateStatus> {
        return try {
            withContext(Dispatchers.IO) {
                async {
                    mutex.withLock {
                        getUpdateCache(context)
                            ?.takeIf { System.currentTimeMillis() - it.objectCreationTimestamp <= CACHE_TIME }
                            ?.let { return@withLock it }

                        findAppUpdateStatus(context)
                    }
                }
            }
        } catch (e: ApiRateLimitExceededException) {
            throw ApiRateLimitExceededException("API rate limit exceeded for $codeName.", e)
        } catch (e: InvalidApiResponseException) {
            throw InvalidApiResponseException("Invalid API response for $codeName.", e)
        } catch (e: NetworkException) {
            throw NetworkException("Network exception for $codeName.", e)
        } catch (e: Exception) {
            throw Exception("Fail to request the latest version of $codeName", e)
        }
    }

    @Throws(NetworkException::class)
    suspend fun checkForUpdateWithoutLoadingFromCacheAsync(context: Context): Deferred<AppUpdateStatus> {
        return try {
            withContext(Dispatchers.IO) {
                async {
                    mutex.withLock {
                        findAppUpdateStatus(context)
                    }
                }
            }
        } catch (e: ApiRateLimitExceededException) {
            throw ApiRateLimitExceededException("API rate limit exceeded for $codeName.", e)
        } catch (e: InvalidApiResponseException) {
            throw InvalidApiResponseException("Invalid API response for $codeName.", e)
        } catch (e: NetworkException) {
            throw NetworkException("Network exception for $codeName.", e)
        } catch (e: Exception) {
            throw Exception("Fail to request the latest version of $codeName", e)
        }
    }

    @Throws(NetworkException::class)
    private suspend fun findAppUpdateStatus(context: Context): AppUpdateStatus {
        val available = findLatestUpdate(context)
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

    open suspend fun convertZipArchiveToApkFile(zipArchive: File, apkFile: File) {
        require(!isDownloadAnApkFile())
        ZipInputStream(zipArchive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry
                    ?: throw RuntimeException("Zip archive does not contain '$fileNameInZipArchive'.")

                if (entry.name == fileNameInZipArchive) {
                    apkFile.outputStream().buffered().use { apk ->
                        zip.copyTo(apk)
                    }
                    @Suppress("BlockingMethodInNonBlockingContext")
                    zip.closeEntry()
                    return
                }

                @Suppress("BlockingMethodInNonBlockingContext")
                zip.closeEntry()
            }
        }
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