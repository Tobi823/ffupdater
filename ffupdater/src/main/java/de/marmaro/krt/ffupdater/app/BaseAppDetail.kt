package de.marmaro.krt.ffupdater.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.*
import java.time.Duration

abstract class BaseAppDetail : AppDetail {
    override fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    protected fun getInstalledVersionFromPackageManager(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    protected fun getInstalledVersionFromSharedPreferences(context: Context, key: String): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(key, null)
    }

    protected fun setInstalledVersionInSharedPreferences(context: Context, key: String, value: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(key, value).apply()
    }

    /**
     * 2min timeout
     */
    override fun updateCheckAsync(context: Context,
                                  deviceEnvironment: DeviceEnvironment): Deferred<UpdateCheckResult> {
        // TODO do not use GlobalScope.async
        return GlobalScope.async(start = CoroutineStart.LAZY) {
            withTimeout(Duration.ofMinutes(2).toMillis()) {
                updateCheck(context, deviceEnvironment)
            }
        }
    }
}