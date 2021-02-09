package de.marmaro.krt.ffupdater.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.device.ABI
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

abstract class BaseApp : App {
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

    override fun updateCheckAsync(context: Context, abi: ABI): Deferred<UpdateCheckResult> {
        return GlobalScope.async(start = CoroutineStart.LAZY) { updateCheck(context, abi) }
    }
}