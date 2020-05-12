package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.utils.Utils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.Q;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

/**
 * This class is a helper to access the settings more easily by checking und converting the raw values from
 * SharedPreferences instance.
 */
public class SettingsHelper {
    public static final boolean DEFAULT_AUTOMATIC_CHECK = true;
    public static final int DEFAULT_CHECK_INTERVAL = 360;
    public static final int DEFAULT_THEME_PREFERENCE = SDK_INT >= Q ? MODE_NIGHT_FOLLOW_SYSTEM : MODE_NIGHT_AUTO_BATTERY;

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

    public static int getThemePreference(Context context) {
        String themePreference = getSharedPreferences(context).getString("themePreference", null);
        if (themePreference == null) {
            return DEFAULT_THEME_PREFERENCE;
        }
        try {
            return Integer.parseInt(themePreference);
        } catch (final NumberFormatException nfe) {
            return DEFAULT_THEME_PREFERENCE;
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}