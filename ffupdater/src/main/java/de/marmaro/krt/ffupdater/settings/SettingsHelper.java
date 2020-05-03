package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;

/**
 * This class is a helper to access the settings more easily by checking und converting the raw values from
 * SharedPreferences instance.
 */
public class SettingsHelper {
    static final int CHECK_INTERVAL_DEFAULT_VALUE = 360;

    /**
     * @param context context
     * @return should the regular background update check be enabled?
     */
    public static boolean isAutomaticCheck(Context context) {
        return getSharedPreferences(context).getBoolean("automaticCheck", true);
    }

    /**
     * @param context context
     * @return how long should be the time span between check background update check?
     */
    public static int getCheckInterval(Context context) {
        String checkInterval = getSharedPreferences(context).getString("checkInterval", null);
        if (checkInterval == null) {
            return CHECK_INTERVAL_DEFAULT_VALUE;
        }
        try {
            return Integer.parseInt(checkInterval);
        } catch (final NumberFormatException nfe) {
            return CHECK_INTERVAL_DEFAULT_VALUE;
        }
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

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}