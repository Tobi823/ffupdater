package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 27.04.2020.
 */
public class SettingsHelper {
    private static final int CHECK_INTERVAL_DEFAULT_VALUE = 15;

    public static boolean isAutomaticCheck(Context context) {
        return getSharedPreferences(context).getBoolean("disableApps", true);
    }

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