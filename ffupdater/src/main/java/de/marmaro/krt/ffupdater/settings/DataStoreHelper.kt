package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.annotation.Keep

@Keep
object DataStoreHelper {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    var lastBackgroundCheck2: Long
        get() = preferences.getLong(LAST_BACKGROUND_CHECK2, 0)
        set(value) = preferences.edit().putLong(LAST_BACKGROUND_CHECK2, value).apply()

    private const val LAST_BACKGROUND_CHECK2 = "lastBackgroundCheck2"
}