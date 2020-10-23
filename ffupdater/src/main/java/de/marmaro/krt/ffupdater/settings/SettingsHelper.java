package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

/**
 * This class is a helper to access the settings more easily by checking und converting the raw values from
 * SharedPreferences instance.
 */
public class SettingsHelper {
    public static final String AUTOMATIC_CHECK = "automaticCheck";
    public static final String CHECK_INTERVAL = "checkInterval";
    public static final String DISABLE_APPS = "disableApps";
    public static final String THEME_PREFERENCE = "themePreference";
    public static final boolean DEFAULT_AUTOMATIC_CHECK = true;
    public static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofHours(6);

    private final SharedPreferences preferences;

    public SettingsHelper(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @return should the regular background update check be enabled?
     */
    public boolean isAutomaticCheck() {
        return preferences.getBoolean(AUTOMATIC_CHECK, DEFAULT_AUTOMATIC_CHECK);
    }

    /**
     * @return how long should be the time span between check background update check?
     */
    public Duration getCheckInterval() {
        final String checkInterval = preferences.getString(CHECK_INTERVAL, null);
        if (checkInterval == null) {
            return DEFAULT_CHECK_INTERVAL;
        }
        try {
            return Duration.ofMinutes(Integer.parseInt(checkInterval));
        } catch (NumberFormatException e) {
            return DEFAULT_CHECK_INTERVAL;
        }
    }

    /**
     * This setting is necessary to deactivate apps when e.g. their update checks are broken.
     *
     * @return the regular background update check should ignore these apps
     */
    public Set<App> getDisableApps() {
        Set<String> disableApps = preferences.getStringSet(DISABLE_APPS, null);
        if (disableApps == null) {
            return new HashSet<>();
        }
        return disableApps.stream()
                .map(this::convertStringToApp)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Optional<App> convertStringToApp(String app) {
        try {
            return Optional.of(App.valueOf(app));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * If the app is started for the first time, the method will return AppCompatDelegate.MODE_NIGHT_NO.
     * If not, then the default value from R.string.default_theme_preference or the user setting will be returned.
     *
     * @return AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, ...
     */
    public int getThemePreference(DeviceEnvironment deviceEnvironment) {
        String themePreference = preferences.getString(THEME_PREFERENCE, null);
        if (themePreference == null) {
            return getDefaultThemePreference(deviceEnvironment);
        }
        try {
            return Integer.parseInt(themePreference);
        } catch (final NumberFormatException nfe) {
            return getDefaultThemePreference(deviceEnvironment);
        }
    }

    private int getDefaultThemePreference(DeviceEnvironment deviceEnvironment) {
        if (deviceEnvironment.getApiLevel() >= Q) {
            return MODE_NIGHT_FOLLOW_SYSTEM;
        } else if (deviceEnvironment.getApiLevel() >= P) {
            return MODE_NIGHT_AUTO_BATTERY;
        } else if (deviceEnvironment.getApiLevel() >= LOLLIPOP) {
            return MODE_NIGHT_AUTO_BATTERY;
        }
        return MODE_NIGHT_NO;
    }
}