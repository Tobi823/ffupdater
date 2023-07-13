package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.annotation.Keep

@Keep
object PowerSettings {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    var howOftenAskedForIgnoringBatteryOptimization
        get() = preferences.getInt("power_settings__how_often_asked_for_ignoring_battery_optimization", 0)
        set(value) {
            preferences
                .edit()
                .putInt("power_settings__how_often_asked_for_ignoring_battery_optimization", value)
                .apply()
        }
}