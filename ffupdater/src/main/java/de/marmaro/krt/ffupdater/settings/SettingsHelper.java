package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.utils.Utils;

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
    public static final boolean DEFAULT_AUTOMATIC_CHECK = true;
    public static final int DEFAULT_CHECK_INTERVAL = 360;
    public static final String AUTOMATIC_CHECK = "automaticCheck";
    public static final String CHECK_INTERVAL = "checkInterval";
    public static final String DISABLE_APPS = "disableApps";
    public static final String THEME_PREFERENCE = "themePreference";

    /**
     * @param context context
     * @return should the regular background update check be enabled?
     */
    public static boolean isAutomaticCheck(Context context) {
        return getSharedPreferences(context).getBoolean(AUTOMATIC_CHECK, DEFAULT_AUTOMATIC_CHECK);
    }

    /**
     * @param context context
     * @return how long should be the time span between check background update check?
     */
    public static int getCheckInterval(Context context) {
        return Utils.stringToInt(getSharedPreferences(context).getString(CHECK_INTERVAL, null), DEFAULT_CHECK_INTERVAL);
    }

    /**
     * This setting is necessary to deactivate apps when e.g. their update checks are broken.
     * @param context context
     * @return the regular background update check should ignore these apps
     */
    public static Set<App> getDisableApps(Context context) {
        Set<App> result = new HashSet<>();
        Set<String> disableApps = getSharedPreferences(context).getStringSet(DISABLE_APPS, null);
        if (disableApps == null) {
            return result;
        }
        for (String disableApp : disableApps) {
            App disableAppObject;
            try {
                disableAppObject = App.valueOf(disableApp);
            } catch (IllegalArgumentException e) {
                continue;
                //ignore
            }
            result.add(disableAppObject);
        }
        return result;
    }

    /**
     * If the app is started for the first time, the method will return AppCompatDelegate.MODE_NIGHT_NO.
     * If not, then the default value from R.string.default_theme_preference or the user setting will be returned.
     * @param context context
     * @return AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, ...
     */
    public static int getThemePreference(Context context, DeviceEnvironment deviceEnvironment) {
        int defaultValue;
        if (deviceEnvironment.getApiLevel() >= Q) {
            defaultValue = MODE_NIGHT_FOLLOW_SYSTEM;
        } else if (deviceEnvironment.getApiLevel() >= P) {
            defaultValue = MODE_NIGHT_AUTO_BATTERY;
        } else if (deviceEnvironment.getApiLevel() >= LOLLIPOP) {
            defaultValue = MODE_NIGHT_AUTO_BATTERY;
        } else {
            defaultValue = MODE_NIGHT_NO;
        }

        String themePreference = getSharedPreferences(context).getString(THEME_PREFERENCE, null);
        if (themePreference == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(themePreference);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}