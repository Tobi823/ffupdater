package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager

class Migrator(private val currentVersionCode: Int = BuildConfig.VERSION_CODE) {

    fun migrate(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastVersionCode = preferences.getInt(FFUPDATER_VERSION_CODE, 0)

        if (lastVersionCode < 87) { // 74.5.0
            deleteOldCacheData(context)
        }
        if (lastVersionCode < 94) { // 75.1.0
            migrateOldSettingsToNewSettings(preferences)
        }

        preferences.edit().putInt(FFUPDATER_VERSION_CODE, currentVersionCode).apply()
    }

    private fun deleteOldCacheData(context: Context) {
        Log.i(LOG_TAG, "Delete old data from cache.")
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles()
            ?.forEach { it.delete() }
        context.externalCacheDir
            ?.listFiles()
            ?.forEach { it.delete() }
    }

    private fun migrateOldSettingsToNewSettings(preferences: SharedPreferences) {
        migrateBooleanSetting(
            preferences,
            "automaticCheck", "background__update_check__enabled",
            defaultValue = true,
            invertValue = false
        )
        migrateBooleanSetting(
            preferences,
            "onlyUnmeteredNetwork", "background__update_check__metered",
            defaultValue = false,
            invertValue = true
        )
        migrateStringSetting(
            preferences,
            "checkInterval",
            "background__update_check__interval"
        )
        migrateSetSetting(
            preferences,
            "disableApps",
            "background__update_check__excluded_apps"
        )
        migrateStringSetting(
            preferences,
            "themePreference",
            "foreground__theme_preference"
        )
    }

    private fun migrateStringSetting(preferences: SharedPreferences, oldKey: String, newKey: String) {
        val value = preferences.getString(oldKey, null)
        preferences.edit().putString(newKey, value).apply()
    }

    private fun migrateSetSetting(preferences: SharedPreferences, oldKey: String, newKey: String) {
        val value = preferences.getStringSet(oldKey, null)
        preferences.edit().putStringSet(newKey, value).apply()
    }

    private fun migrateBooleanSetting(
        preferences: SharedPreferences,
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