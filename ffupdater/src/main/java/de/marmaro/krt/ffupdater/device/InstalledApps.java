package de.marmaro.krt.ffupdater.device;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.marmaro.krt.ffupdater.App;

/**
 * Detect installed apps and their version names.
 */
public class InstalledApps {
    /**
     * Get the version name of app. If the app is not installed, an empty string will be returned.
     * @param packageManager
     * @param app
     * @return version name or empty string.
     */
    @NonNull
    public static String getVersionName(PackageManager packageManager, App app) {
        try {
            return packageManager.getPackageInfo(app.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /**
     * @param packageManager
     * @param app
     * @return is app installed
     */
    public static boolean isInstalled(PackageManager packageManager, App app) {
        return !getVersionName(packageManager, app).isEmpty();
    }

    /**
     * @param packageManager
     * @return all installed apps
     */
    @NonNull
    public static List<App> getInstalledApps(PackageManager packageManager) {
        List<App> installedApps = new ArrayList<>();
        for (App app : App.values()) {
            if (isInstalled(packageManager, app)) {
                installedApps.add(app);
            }
        }
        return installedApps;
    }

    /**
     * @param packageManager
     * @return all not installed apps
     */
    @NonNull
    public static List<App> getNotInstalledApps(PackageManager packageManager) {
        List<App> apps = new ArrayList<>(Arrays.asList(App.values()));
        apps.removeAll(getInstalledApps(packageManager));
        return apps;
    }
}