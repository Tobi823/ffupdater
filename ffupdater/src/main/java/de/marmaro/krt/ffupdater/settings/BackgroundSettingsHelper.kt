package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import java.time.Duration

@Keep
object BackgroundSettingsHelper {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    val isUpdateCheckEnabled
        get() = preferences.getBoolean("background__update_check__enabled", true)

    val useDifferentNotificationChannels
        get() = preferences.getBoolean("background__separate_notification_channels", false)

    val updateCheckInterval: Duration
        get() {
            val minutes = preferences.getString("background__update_check__interval", null)?.toLongOrNull()
            // keep minutes in range between 15 minutes and 40320 minutes (1 month)
            // if undefined or invalid -> use 360 minutes (6 hours)
            val result = minutes?.coerceIn(15L, 40320L) ?: 360L
            return Duration.ofMinutes(result)
        }

    /**
     * This setting is necessary to deactivate apps when e.g. their update checks are broken.
     *
     * @return the regular background update check should ignore these apps
     */
    val excludedAppsFromUpdateCheck: List<App>
        get() {
            val disableApps = preferences.getStringSet("background__update_check__excluded_apps", null)
                ?: setOf()
            return disableApps.mapNotNull {
                try {
                    App.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }

    val isUpdateCheckOnMeteredAllowed
        get() = preferences.getBoolean("background__update_check__metered", true)

    val isUpdateCheckOnlyAllowedWhenDeviceIsIdle
        get() = preferences.getBoolean("background__update_check__when_device_idle", false)

    val isDownloadEnabled
        get() = preferences.getBoolean("background__download__enabled", true)

    val isDownloadOnMeteredAllowed
        get() = preferences.getBoolean("background__download__metered", false)

    val isInstallationEnabled
        get() = preferences.getBoolean("background__installation__enabled", false)

    val isDeleteUpdateIfInstallSuccessful
        get() = preferences.getBoolean("background__delete_cache_if_install_successful", true)

    val isDeleteUpdateIfInstallFailed
        get() = preferences.getBoolean("background__delete_cache_if_install_failed", false)
}