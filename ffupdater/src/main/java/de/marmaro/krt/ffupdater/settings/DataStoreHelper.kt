package de.marmaro.krt.ffupdater.settings

import android.content.Context
import androidx.preference.PreferenceManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeParseException

class DataStoreHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var lastBackgroundCheck: LocalDateTime?
        get() {
            return try {
                preferences.getString(LAST_BACKGROUND_CHECK_TIMESTAMP, null)
                    ?.let { str -> LocalDateTime.parse(str, ISO_LOCAL_DATE_TIME) }
            } catch (e: DateTimeParseException) {
                null
            }
        }
        set(value) {
            val timestamp = value?.format(ISO_LOCAL_DATE_TIME)
            preferences.edit()
                .putString(LAST_BACKGROUND_CHECK_TIMESTAMP, timestamp)
                .apply()
        }

    val lastBackgroundCheckString: String
        get() {
            return lastBackgroundCheck
                ?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(it) }
                ?: "/"
        }

    companion object {
        const val LAST_BACKGROUND_CHECK_TIMESTAMP = "lastBackgroundCheckTimestamp"
    }
}