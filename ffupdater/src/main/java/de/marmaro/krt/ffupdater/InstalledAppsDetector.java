package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by Tobiwan on 21.08.2019.
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

    String getVersionName(App app) {
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

    boolean isInstalled(App app) {
        return !getVersionName(app).isEmpty();
    }

    List<App> getInstalledApps() {
        List<App> installedApps = new ArrayList<>();
        if (isInstalled(App.FENNEC_RELEASE)) {
            installedApps.add(App.FENNEC_RELEASE);
        }
        if (isInstalled(App.FENNEC_BETA)) {
            installedApps.add(App.FENNEC_BETA);
        }
        if (isInstalled(App.FENNEC_NIGHTLY)) {
            installedApps.add(App.FENNEC_NIGHTLY);
        }
        if (isInstalled(App.FIREFOX_KLAR)) {
            installedApps.add(App.FIREFOX_KLAR);
        }
        if (isInstalled(App.FIREFOX_FOCUS)) {
            installedApps.add(App.FIREFOX_FOCUS);
        }
        if (isInstalled(App.FIREFOX_LITE)) {
            installedApps.add(App.FIREFOX_LITE);
        }
        if (isInstalled(App.FENIX)) {
            installedApps.add(App.FENIX);
        }
        return installedApps;
    }

    public List<App> getNotInstalledApps() {
        List<App> allApps = new ArrayList<>(Arrays.asList(App.values()));
        allApps.removeAll(getInstalledApps());
        return allApps;
    }

    @Contract(value = "null -> !null", pure = true)
    private String getVersionName(PackageInfo packageInfo) {
        return packageInfo == null ? "" : packageInfo.versionName;
    }

    @Nullable
    private PackageInfo findPackageInfo(String packageName) {
        try {
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}