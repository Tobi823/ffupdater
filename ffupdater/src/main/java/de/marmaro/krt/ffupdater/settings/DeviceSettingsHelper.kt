package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


class DeviceSettingsHelper {
    private val preferences: SharedPreferences


    constructor(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    val prefer32BitApks
        get() = preferences.getBoolean("device__prefer_32bit_apks", false)
}