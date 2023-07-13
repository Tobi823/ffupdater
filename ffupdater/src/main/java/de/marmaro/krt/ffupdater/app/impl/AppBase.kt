package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.FFUpdater
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.app.impl.base.ApkDownloader
import de.marmaro.krt.ffupdater.app.impl.base.AppAttributes
import de.marmaro.krt.ffupdater.app.impl.base.InstalledVersion
import de.marmaro.krt.ffupdater.app.impl.base.UpdateFetcher
import de.marmaro.krt.ffupdater.app.impl.base.VersionDisplay
import de.marmaro.krt.ffupdater.device.ABI.ARM64_V8A
import de.marmaro.krt.ffupdater.device.ABI.ARMEABI
import de.marmaro.krt.ffupdater.device.ABI.ARMEABI_V7A
import de.marmaro.krt.ffupdater.device.ABI.MIPS
import de.marmaro.krt.ffupdater.device.ABI.MIPS64
import de.marmaro.krt.ffupdater.device.ABI.X86
import de.marmaro.krt.ffupdater.device.ABI.X86_64
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour


@Keep
abstract class AppBase : AppAttributes, ApkDownloader, UpdateFetcher, InstalledVersion, VersionDisplay {
    override val installationWarning: Int? = null
    override val installableByUser = true
    override val eolReason: Int? = null
    override val fileNameInZipArchive: String? = null


    @AnyThread
    open fun isAvailableVersionHigherThanInstalled(context: Context, available: LatestUpdate): Boolean {
        val installedVersion = getInstalledVersion(context.packageManager) ?: return true
        return VersionCompareHelper.isAvailableVersionHigher(installedVersion, available.version)
    }

    suspend fun findAppUpdateStatus(context: Context, cacheBehaviour: CacheBehaviour): AppUpdateStatus {
        val available = try {
            Log.d(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Search for latest ${app.name} update.")
            val time = System.currentTimeMillis()
            val available = fetchLatestUpdate(context.applicationContext, cacheBehaviour)
            val duration = System.currentTimeMillis() - time
            Log.i(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Found ${app.name} ${available.version} (${duration}ms).")
            available
        } catch (e: NetworkException) {
            Log.d(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Can't find latest update for ${app.name}.", e)
            throw NetworkException("can't find latest update for ${app.name}.", e)
        } catch (e: DisplayableException) {
            Log.d(FFUpdater.LOG_TAG, "findAppUpdateStatus(): Can't find latest update for ${app.name}.", e)
            throw DisplayableException("can't find latest update for ${app.name}.", e)
        }
        return AppUpdateStatus(
            latestUpdate = available,
            isUpdateAvailable = isAvailableVersionHigherThanInstalled(context.applicationContext, available),
            displayVersion = getDisplayAvailableVersion(context.applicationContext, available)
        )
    }

    @AnyThread
    open fun installCallback(context: Context, available: AppUpdateStatus) {
    }


    companion object {
        val ALL_ABIS = listOf(ARM64_V8A, ARMEABI_V7A, ARMEABI, X86_64, X86, MIPS, MIPS64)
        val ARM32_ARM64_X86_X64 = listOf(ARM64_V8A, ARMEABI_V7A, X86_64, X86)
        val ARM32_ARM64 = listOf(ARM64_V8A, ARMEABI_V7A)
        val ARM32_X86 = listOf(ARMEABI_V7A, X86)
    }
}