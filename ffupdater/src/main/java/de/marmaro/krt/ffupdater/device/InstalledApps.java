package de.marmaro.krt.ffupdater.device;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import de.marmaro.krt.ffupdater.App;

/**
 * Detect installed apps and their version names.
 */
public class InstalledApps {
    private final PackageManager packageManager;

    public InstalledApps(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    /**
     * Get the version name of app. If the app is not installed, an empty string will be returned.
     * @param app
     * @return version name or empty string.
     */
    @NonNull
    public String getVersionName(App app) {
        try {
            return packageManager.getPackageInfo(app.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /**
     * @param app
     * @return if the app is installed or not.
     */
    public boolean isInstalled(App app) {
        return !getVersionName(app).isEmpty();
    }

    /**
     * @return all installed apps.
     */
    public List<App> getInstalledApps() {
        List<App> installedApps = new ArrayList<>();
        for (App app : App.values()) {
            if (isInstalled(app)) {
                installedApps.add(app);
            }
        }
        return installedApps;
    }

    /**
     * @return all not installed apps.
     */
    public List<App> getNotInstalledApps() {
        List<App> notInstalledApp = new ArrayList<>();
        for (App app : App.values()) {
            if (!isInstalled(app)) {
                notInstalledApp.add(app);
            }
        }
        return notInstalledApp;
    }
}