package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate

class Migrator(private val currentVersionCode: Int = BuildConfig.VERSION_CODE) {

    fun migrate(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastVersionCode = preferences.getInt(FFUPDATER_VERSION_CODE, 0)

        if (lastVersionCode < 56 /* 71.0.0 */) {
            deleteAvailableMetadataCache(preferences)
        }

        preferences.edit().putInt(FFUPDATER_VERSION_CODE, currentVersionCode).apply()
    }

    private fun deleteAvailableMetadataCache(preferences: SharedPreferences) {
        val keys = preferences.all.filter { it.key.startsWith("download_metadata_") }
                .map { it.key }
        val editor = preferences.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }

    companion object {
        const val FFUPDATER_VERSION_CODE = "migrator_ffupdater_version_code"
    }
}