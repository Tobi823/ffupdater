package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Detect installed apps and their version names.
 */
public class InstalledAppsDetector {
    private static final String FENNEC_RELEASE = "org.mozilla.firefox";
    private static final String FENNEC_BETA = "org.mozilla.firefox_beta";
    private static final String FENNEC_NIGHTLY = "org.mozilla.fennec_aurora";
    private static final String FIREFOX_KLAR = "org.mozilla.klar";
    private static final String FIREFOX_FOCUS = "org.mozilla.focus";
    private static final String FIREFOX_LITE = "org.mozilla.rocket";
    private static final String FENIX = "org.mozilla.fenix";

    private PackageManager packageManager;

    public InstalledAppsDetector(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    /**
     * Get the version name of app. If the app is not installed, an empty string will be returned.
     * @param app
     * @return version name or empty string.
     */
    public String getVersionName(App app) {
        switch (app) {
            case FENNEC_RELEASE:
                return getVersionName(findPackageInfo(FENNEC_RELEASE));
            case FENNEC_BETA:
                return getVersionName(findPackageInfo(FENNEC_BETA));
            case FENNEC_NIGHTLY:
                return getVersionName(findPackageInfo(FENNEC_NIGHTLY));
            case FIREFOX_KLAR:
                return getVersionName(findPackageInfo(FIREFOX_KLAR));
            case FIREFOX_FOCUS:
                return getVersionName(findPackageInfo(FIREFOX_FOCUS));
            case FIREFOX_LITE:
                return getVersionName(findPackageInfo(FIREFOX_LITE));
            case FENIX:
                return getVersionName(findPackageInfo(FENIX));
        }
        throw new IllegalArgumentException("Unknown parameter");
    }

    /**
     * @param app
     * @return if the app is installed or not.
     */
    boolean isInstalled(App app) {
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

    /**
     * Internal method for getting the nonnull version name.
     * @param packageInfo
     * @return
     */
    @Contract(value = "null -> !null", pure = true)
    private String getVersionName(PackageInfo packageInfo) {
        return packageInfo == null ? "" : packageInfo.versionName;
    }

    /**
     * Internal method for getting the PackageInfo of the given packageName.
     * @param packageName
     * @return
     */
    @Nullable
    private PackageInfo findPackageInfo(String packageName) {
        try {
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}