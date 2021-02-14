package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class Migrator(private val currentVersionCode: Int = BuildConfig.VERSION_CODE) {

    fun migrate(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastVersionCode = preferences.getInt(FFUPDATER_VERSION_CODE, 0)

        if (lastVersionCode < 56 /* 71.0.0 */) {
            deleteDeprecatedData(preferences)
        }

        preferences.edit().putInt(FFUPDATER_VERSION_CODE, currentVersionCode).apply()
    }

    private fun deleteDeprecatedData(preferences: SharedPreferences) {
        val keys = preferences.all.filter {
            it.key.startsWith("download_metadata_") or
                    it.key.startsWith("device_app_register_")
        }.map { it.key }
        val editor = preferences.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }

    companion object {
        const val FFUPDATER_VERSION_CODE = "migrator_ffupdater_version_code"
    }
}