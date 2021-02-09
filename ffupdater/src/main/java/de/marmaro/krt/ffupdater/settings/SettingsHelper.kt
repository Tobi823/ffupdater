package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.os.Build.VERSION_CODES.*
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AppList
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import java.time.Duration
import kotlin.Int.Companion.MAX_VALUE

/**
 * This class is a helper to access the settings more easily by checking und converting the raw values from
 * SharedPreferences instance.
 */
class SettingsHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * @return should the regular background update check be enabled?
     */
    val automaticCheck: Boolean
        get() {
            return preferences.getBoolean("automaticCheck", true)
        }

    /**
     * @return how long should be the time span between check background update check?
     */
    val checkInterval: Duration
        get() {
            val default = Duration.ofHours(6)
            val checkInterval = preferences.getString("checkInterval", null)
            return try {
                Duration.ofMinutes(checkInterval?.toLong() ?: return default)
            } catch (_: NumberFormatException) {
                default
            }
        }

    /**
     * This setting is necessary to deactivate apps when e.g. their update checks are broken.
     *
     * @return the regular background update check should ignore these apps
     */
    val disabledApps: List<AppList>
        get() {
            val disableApps = preferences.getStringSet("disableApps", null) ?: setOf()
            return disableApps.mapNotNull {
                try {
                    AppList.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }

    /**
     * If the app is started for the first time, the method will return AppCompatDelegate.MODE_NIGHT_NO.
     * If not, then the default value from R.string.default_theme_preference or the user setting will be returned.
     *
     * @return AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, ...
     */
    fun getThemePreference(deviceEnvironment: DeviceEnvironment): Int {
        val default = when (deviceEnvironment.sdkInt) {
            in Q..MAX_VALUE -> MODE_NIGHT_FOLLOW_SYSTEM
            in LOLLIPOP..P -> MODE_NIGHT_AUTO_BATTERY
            else -> throw Exception("invalid API level")
        }

        // TODO die preference keys verallgemeinern, damit kein Rechtschreibfehler möglich ist
        val themePreference = preferences.getString("themePreference", null)
        return try {
            themePreference?.toInt() ?: default
        } catch (_: NumberFormatException) {
            default
        }
    }
}