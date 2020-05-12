package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.utils.Utils;

/**
 * This class is a helper to access the settings more easily by checking und converting the raw values from
 * SharedPreferences instance.
 */
public class SettingsHelper {
    public static final boolean DEFAULT_AUTOMATIC_CHECK = true;
    public static final int DEFAULT_CHECK_INTERVAL = 360;

    /**
     * @param context context
     * @return should the regular background update check be enabled?
     */
    public static boolean isAutomaticCheck(Context context) {
        return getSharedPreferences(context).getBoolean("automaticCheck", DEFAULT_AUTOMATIC_CHECK);
    }

    /**
     * @param context context
     * @return how long should be the time span between check background update check?
     */
    public static int getCheckInterval(Context context) {
        return Utils.stringToInt(getSharedPreferences(context).getString("checkInterval", null), DEFAULT_CHECK_INTERVAL);
    }

    /**
     * This setting is necessary to deactivate apps when e.g. their update checks are broken.
     * @param context context
     * @return the regular background update check should ignore these apps
     */
    public static Set<App> getDisableApps(Context context) {
        Set<App> result = new HashSet<>();
        Set<String> disableApps = getSharedPreferences(context).getStringSet("disableApps", null);
        if (disableApps == null) {
            return result;
        }
        for (String disableApp : disableApps) {
            result.add(App.valueOf(disableApp));
        }
        return result;
    }

    /**
     * If the app is started for the first time, the method will return AppCompatDelegate.MODE_NIGHT_NO.
     * If not, then the default value from R.string.default_theme_preference or the user setting will be returned.
     * @param context context
     * @return AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, ...
     */
    public static int getThemePreference(Context context) {
        String themePreference = getSharedPreferences(context).getString("themePreference", null);
        if (themePreference == null) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        try {
            return Integer.parseInt(themePreference);
        } catch (final NumberFormatException nfe) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}