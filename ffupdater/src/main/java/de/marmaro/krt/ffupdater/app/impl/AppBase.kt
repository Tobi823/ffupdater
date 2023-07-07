package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.ABI.ARM64_V8A
import de.marmaro.krt.ffupdater.device.ABI.ARMEABI
import de.marmaro.krt.ffupdater.device.ABI.ARMEABI_V7A
import de.marmaro.krt.ffupdater.device.ABI.MIPS
import de.marmaro.krt.ffupdater.device.ABI.MIPS64
import de.marmaro.krt.ffupdater.device.ABI.X86
import de.marmaro.krt.ffupdater.device.ABI.X86_64
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.security.FingerprintValidator
import de.marmaro.krt.ffupdater.security.PackageManagerUtil


@Keep
abstract class AppBase {
    abstract val app: App
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

    @AnyThread
    suspend fun isInstalled(context: Context): InstallationStatus {
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
    fun isInstalledWithoutFingerprintVerification(context: Context): Boolean {
        return PackageManagerUtil(context.packageManager).isAppInstalled(packageName)
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

    fun isAppPublishedAsZipArchive() = (fileNameInZipArchive != null)

    @AnyThread
    open fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
    }

    suspend fun findAppUpdateStatus(context: Context, cacheBehaviour: CacheBehaviour): AppUpdateStatus {
        val time = System.currentTimeMillis()
        Log.d(LOG_TAG, "find latest update for ${app.name}")
        val available = try {
            findLatestUpdate(context.applicationContext, cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("can't find latest update for ${app.name}", e)
        } catch (e: DisplayableException) {
            throw DisplayableException("can't find latest update for ${app.name}", e)
        }
        val duration = System.currentTimeMillis() - time
        Log.i(LOG_TAG, "find latest update ${available.version} for $app after $duration ms")
        return AppUpdateStatus(
            latestUpdate = available,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context, available),
            displayVersion = getDisplayAvailableVersion(context, available)
        )
    }

    @WorkerThread
    internal abstract suspend fun findLatestUpdate(
        context: Context,
        cacheBehaviour: CacheBehaviour,
    ): LatestUpdate

    @AnyThread
    open fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate,
    ): Boolean {
        val installedVersion = getInstalledVersion(context) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    companion object {
        val ALL_ABIS = listOf(ARM64_V8A, ARMEABI_V7A, ARMEABI, X86_64, X86, MIPS, MIPS64)
        val ARM32_ARM64_X86_X64 = listOf(ARM64_V8A, ARMEABI_V7A, X86_64, X86)
        val ARM32_ARM64 = listOf(ARM64_V8A, ARMEABI_V7A)
        val ARM32_X86 = listOf(ARMEABI_V7A, X86)
        const val LOG_TAG = "AppBase"
    }
}