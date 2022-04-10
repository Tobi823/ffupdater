package de.marmaro.krt.ffupdater

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager

class Migrator(
    private val context: Context,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE
) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun migrate() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastVersionCode = preferences.getInt(FFUPDATER_VERSION_CODE, 0)

        if (lastVersionCode < 87) { // 74.5.0
            deleteOldCacheData()
        }
        if (lastVersionCode < 94) { // 75.1.0
            migrateOldSettingsToNewSettings()
        }

        preferences.edit().putInt(FFUPDATER_VERSION_CODE, currentVersionCode).apply()
    }

    private fun deleteOldCacheData() {
        Log.i(LOG_TAG, "Delete old data from cache.")
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles()
            ?.forEach { it.delete() }
        context.externalCacheDir
            ?.listFiles()
            ?.forEach { it.delete() }
    }

    private fun migrateOldSettingsToNewSettings() {
        migrateBooleanSetting(
            "automaticCheck", "background__update_check__enabled",
            defaultValue = true,
            invertValue = false
        )
        migrateBooleanSetting(
            "onlyUnmeteredNetwork", "background__update_check__metered",
            defaultValue = false,
            invertValue = true
        )
        migrateStringSetting("checkInterval", "background__update_check__interval")
        migrateSetSetting("disableApps", "background__update_check__excluded_apps")
        migrateStringSetting("themePreference", "foreground__theme_preference")

    }

    private fun migrateStringSetting(oldKey: String, newKey: String) {
        val value = preferences.getString(oldKey, null)
        preferences.edit().putString(newKey, value).apply()
    }

    private fun migrateSetSetting(oldKey: String, newKey: String) {
        val value = preferences.getStringSet(oldKey, null)
        preferences.edit().putStringSet(newKey, value).apply()
    }

    private fun migrateBooleanSetting(
        oldKey: String,
        newKey: String,
        defaultValue: Boolean,
        invertValue: Boolean
    ) {
        var value = preferences.getBoolean(oldKey, defaultValue)
        if (invertValue) {
            value = !value
        }
        preferences.edit().putBoolean(newKey, value).apply()
    }


    companion object {
        const val FFUPDATER_VERSION_CODE = "migrator_ffupdater_version_code"
        const val LOG_TAG = "Migrator"
    }
}