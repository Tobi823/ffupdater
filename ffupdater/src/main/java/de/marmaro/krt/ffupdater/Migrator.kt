package de.marmaro.krt.ffupdater

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager

class Migrator(private val currentVersionCode: Int = BuildConfig.VERSION_CODE) {

    fun migrate(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastVersionCode = preferences.getInt(FFUPDATER_VERSION_CODE, 0)

        if (lastVersionCode < 87 /* TODO */) {
            deleteOldCacheData(context)
        }

        preferences.edit().putInt(FFUPDATER_VERSION_CODE, currentVersionCode).apply()
    }

    private fun deleteOldCacheData(context: Context) {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles()
            ?.forEach { it.delete() }
        context.externalCacheDir
            ?.listFiles()
            ?.forEach { it.delete() }
    }

    companion object {
        const val FFUPDATER_VERSION_CODE = "migrator_ffupdater_version_code"
    }
}