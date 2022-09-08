package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class NetworkSettingsHelper {
    private val preferences: SharedPreferences

    constructor(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    val areUserCAsTrusted
        get() = preferences.getBoolean("network__trust_user_cas", false)
}