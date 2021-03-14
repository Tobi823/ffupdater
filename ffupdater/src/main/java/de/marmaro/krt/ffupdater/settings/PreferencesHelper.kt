package de.marmaro.krt.ffupdater.settings

import android.content.Context
import androidx.preference.PreferenceManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class PreferencesHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var lastBackgroundCheck: LocalDateTime?
        get() {
            val timestamp = preferences.getString(LAST_BACKGROUND_CHECK_TIMESTAMP, null)
                    ?: return null
            return try {
                LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: DateTimeParseException) {
                null
            }
        }
        set(value) {
            val timestamp = value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            preferences.edit()
                    .putString(LAST_BACKGROUND_CHECK_TIMESTAMP, timestamp)
                    .apply()
        }

    companion object {
        const val LAST_BACKGROUND_CHECK_TIMESTAMP = "lastBackgroundCheckTimestamp"
    }
}