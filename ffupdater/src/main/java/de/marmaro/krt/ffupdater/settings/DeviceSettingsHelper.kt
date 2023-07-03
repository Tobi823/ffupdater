package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences


object DeviceSettingsHelper {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    val prefer32BitApks
        get() = preferences.getBoolean("device__prefer_32bit_apks", false)
}