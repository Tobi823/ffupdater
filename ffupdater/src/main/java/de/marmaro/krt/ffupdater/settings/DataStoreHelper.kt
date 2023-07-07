package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.annotation.Keep
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeParseException

@Keep
object DataStoreHelper {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }


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

    fun getLastBackgroundCheckString(context: Context): String {
        return DateUtils.getRelativeDateTimeString(
            context.applicationContext,
            (lastBackgroundCheck?.toEpochSecond() ?: return "/") * 1000,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        ).toString()
    }

    const val LAST_BACKGROUND_CHECK_TIMESTAMP = "lastBackgroundCheckTimestamp"
}