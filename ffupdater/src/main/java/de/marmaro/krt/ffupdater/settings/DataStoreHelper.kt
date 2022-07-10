package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeParseException

class DataStoreHelper(val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var lastBackgroundCheck: ZonedDateTime?
        get() {
            return try {
                preferences.getString(LAST_BACKGROUND_CHECK_TIMESTAMP, null)
                    ?.let { str -> ZonedDateTime.parse(str, ISO_OFFSET_DATE_TIME) }
            } catch (e: DateTimeParseException) {
                null
            }
        }
        set(value) {
            preferences.edit()
                .putString(LAST_BACKGROUND_CHECK_TIMESTAMP, value?.format(ISO_OFFSET_DATE_TIME))
                .apply()
        }

    val lastBackgroundCheckString: String
        get() {
            return DateUtils.getRelativeDateTimeString(
                context,
                (lastBackgroundCheck?.toEpochSecond() ?: return "/") * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
            ).toString()
        }

    companion object {
        const val LAST_BACKGROUND_CHECK_TIMESTAMP = "lastBackgroundCheckTimestamp"
    }
}