package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;

/**
 * Created by Tobiwan on 27.04.2020.
 */
public class SettingsHelper {

    public static boolean isAutomaticCheck(Context context) {
        return getSharedPreferences(context).getBoolean("disableApps", true);
    }

    public static int getCheckInterval(Context context) {
        String checkInterval = getSharedPreferences(context).getString("checkInterval", null);
        return NumberUtils.toInt(checkInterval, 15);
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
