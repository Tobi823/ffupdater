package de.marmaro.krt.ffupdater.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import java.time.Duration

/**
 * This class is a helper to access the settings more easily by checking und converting the raw values from
 * SharedPreferences instance.
 */
class SettingsHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val isForegroundUpdateCheckOnMeteredAllowed: Boolean
        get() = preferences.getBoolean("foreground__update_check__metered", true)

    val isForegroundDownloadOnMeteredAllowed: Boolean
        get() = preferences.getBoolean("foreground__download__metered", true)

    private val validAndroidThemes = listOf(
        MODE_NIGHT_FOLLOW_SYSTEM,
        MODE_NIGHT_AUTO_BATTERY,
        MODE_NIGHT_YES,
        MODE_NIGHT_NO
    )

    val themePreference: Int
        get() {
            val theme = preferences.getString("foreground__theme_preference", null)?.toIntOrNull()
            return when {
                theme in validAndroidThemes -> theme!!
                // return default values because theme is invalid and could be null
                DeviceSdkTester.supportsAndroid10() -> MODE_NIGHT_FOLLOW_SYSTEM
                else -> MODE_NIGHT_AUTO_BATTERY
            }
        }

    /**
     * @return should the regular background update check be enabled?
     */
    val isBackgroundUpdateCheckEnabled: Boolean
        get() = preferences.getBoolean("background__update_check__enabled", true)

    /**
     * @return how long should be the time span between check background update check?
     */
    val backgroundUpdateCheckInterval: Duration
        get() {
            val minutes = preferences.getString("background__update_check__interval", null)?.toLongOrNull()
            // keep minutes in range between 15 minutes and 40320 minutes (1 month)
            // if undefined or invalid -> use 360 minutes (6 hours)
            val result = minutes?.coerceIn(15L, 40320L) ?: 360L
            return Duration.ofMinutes(result)
        }

    val isBackgroundUpdateCheckOnMeteredAllowed: Boolean
        get() = preferences.getBoolean("background__update_check__metered", true)

    val isBackgroundDownloadEnabled: Boolean
        get() = preferences.getBoolean("background__download__enabled", true)

    val isBackgroundDownloadOnMeteredAllowed: Boolean
        get() = preferences.getBoolean("background__download__metered", false)

    val isBackgroundInstallationEnabled: Boolean
        get() = preferences.getBoolean("background__installation__enabled", false)

    /**
     * This setting is necessary to deactivate apps when e.g. their update checks are broken.
     *
     * @return the regular background update check should ignore these apps
     */
    val excludedAppsFromBackgroundUpdateCheck: List<App>
        get() {
            val disableApps = preferences.getStringSet("background__update_check__excluded_apps", null) ?: setOf()
            return disableApps.mapNotNull {
                try {
                    App.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
}