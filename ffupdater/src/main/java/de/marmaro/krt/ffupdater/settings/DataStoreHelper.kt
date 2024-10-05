package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.annotation.Keep
import java.time.Duration

@Keep
object DataStoreHelper {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    var lastAppBackgroundCheck: Long
        get() = preferences.getLong(LAST_APP_BACKGROUND_CHECK, 0)
        private set(value) = preferences.edit().putLong(LAST_APP_BACKGROUND_CHECK, value).apply()

    fun storeThatAllAppsHasBeenChecked() {
        lastAppBackgroundCheck = System.currentTimeMillis()
    }

    fun getDurationSinceAllAppsHasBeenChecked(): Duration? {
        val ms = lastAppBackgroundCheck
        if (ms == 0L) {
            return null
        }
        return Duration.ofMillis(System.currentTimeMillis() - ms)
    }

    private var lastBackgroundCheckTrigger: Long
        get() = preferences.getLong(LAST_BACKGROUND_CHECK_TRIGGER, 0)
        set(value) = preferences.edit().putLong(LAST_BACKGROUND_CHECK_TRIGGER, value).apply()

    fun storeThatBackgroundCheckWasTrigger() {
        lastBackgroundCheckTrigger = System.currentTimeMillis()
    }

    fun getDurationSinceBackgroundCheckWasTriggered(): Duration? {
        val ms = lastBackgroundCheckTrigger
        if (ms == 0L) {
            return null
        }
        return Duration.ofMillis(System.currentTimeMillis() - ms)
    }

    private const val LAST_APP_BACKGROUND_CHECK = "lastAppBackgroundCheck"
    private const val LAST_BACKGROUND_CHECK_TRIGGER = "lastBackgroundCheckTrigger"
}