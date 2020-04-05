package de.marmaro.krt.ffupdater;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.marmaro.krt.ffupdater.download.fennec.MozillaFtp;
import de.marmaro.krt.ffupdater.download.fennec.OfficialApi;
import de.marmaro.krt.ffupdater.download.github.FenixVersionFinder;
import de.marmaro.krt.ffupdater.download.github.FirefoxLiteVersionFinder;
import de.marmaro.krt.ffupdater.download.github.FocusVersionFinder;

import static de.marmaro.krt.ffupdater.App.FENIX;
import static de.marmaro.krt.ffupdater.App.FENNEC_BETA;
import static de.marmaro.krt.ffupdater.App.FENNEC_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FENNEC_RELEASE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;

/**
 * Retrieve the latest version names for a specific set of apps.
 * Get the download url for a specific app and compare version names to determine an available update.
 */
public class AvailableApps {
    private Map<App, String> versions = new HashMap<>();
    private Map<App, String> downloadUrl = new HashMap<>();
    private boolean triggerDownload = false;
    private App appToDownload;

    private AvailableApps() {
    }

    /**
     * Request APIs for retrieving the latest version names for {@code appsToCheck}.
     * Set flag to download a specific app.
     * The app that will be downloaded will be automatically added to {@code appsToCheck}.
     *
     * @param appsToCheck   apps for update checks
     * @param appToDownload app for download
     * @return object containing all version names for {@code appsToCheck}.
     */
    static AvailableApps createAndTriggerDownload(Set<App> appsToCheck, App appToDownload) {
        Set<App> copiedAppsToCheck = new HashSet<>(appsToCheck);
        copiedAppsToCheck.add(appToDownload);

        AvailableApps availableApps = create(copiedAppsToCheck);
        availableApps.triggerDownload = true;
        availableApps.appToDownload = appToDownload;
        return availableApps;
    }

    /**
     * Request APIs for retrieving the latest version names for {@code appsToCheck}.
     *
     * @param appsToCheck apps for update check
     * @return object containing all version names for {@code appsToCheck}.
     */
    public static AvailableApps create(Set<App> appsToCheck) {
        AvailableApps result = new AvailableApps();

        if (appsToCheck.contains(FENNEC_RELEASE) ||
                appsToCheck.contains(FENNEC_BETA) ||
                appsToCheck.contains(FENNEC_NIGHTLY)) {
            checkFennec(result, appsToCheck);
        }

        if (appsToCheck.contains(FIREFOX_KLAR) ||
                appsToCheck.contains(FIREFOX_FOCUS)) {
            checkFocusKlar(result, appsToCheck);
        }

        if (appsToCheck.contains(FIREFOX_LITE)) {
            checkLite(result);
        }

        if (appsToCheck.contains(FENIX)) {
            checkFenix(result);
        }
        return result;
    }

    private static void checkFennec(AvailableApps result, Set<App> appsToCheck) {
        Optional<OfficialApi.Version> response = OfficialApi.getResponse();
        if (!response.isPresent()) {
            return;
        }

        OfficialApi.Version version = response.get();
        if (appsToCheck.contains(FENNEC_RELEASE)) {
            result.versions.put(FENNEC_RELEASE, version.getReleaseVersion());
            result.downloadUrl.put(FENNEC_RELEASE, getDownloadUrlForFennec(FENNEC_RELEASE, version));
        }
        if (appsToCheck.contains(FENNEC_BETA)) {
            result.versions.put(FENNEC_BETA, version.getBetaVersion());
            result.downloadUrl.put(FENNEC_BETA, getDownloadUrlForFennec(FENNEC_BETA, version));
        }
        if (appsToCheck.contains(FENNEC_NIGHTLY)) {
            result.versions.put(FENNEC_NIGHTLY, version.getNightlyVersion());
            result.downloadUrl.put(FENNEC_NIGHTLY, getDownloadUrlForFennec(FENNEC_NIGHTLY, version));
        }
    }

    private static void checkFocusKlar(AvailableApps result, Set<App> appsToCheck) {
        FocusVersionFinder focusVersionFinder = FocusVersionFinder.create();
        if (!focusVersionFinder.isCorrect()) {
            return;
        }

        if (appsToCheck.contains(FIREFOX_FOCUS)) {
            result.versions.put(FIREFOX_FOCUS, focusVersionFinder.getVersion());
            result.downloadUrl.put(FIREFOX_FOCUS, focusVersionFinder.getDownloadUrl(FIREFOX_FOCUS, DeviceABI.getAbi()));
        }

        if (appsToCheck.contains(FIREFOX_KLAR)) {
            result.versions.put(FIREFOX_KLAR, focusVersionFinder.getVersion());
            result.downloadUrl.put(FIREFOX_KLAR, focusVersionFinder.getDownloadUrl(FIREFOX_KLAR, DeviceABI.getAbi()));
        }
    }

    private static void checkLite(AvailableApps result) {
        FirefoxLiteVersionFinder firefoxLiteVersionFinder = FirefoxLiteVersionFinder.create();
        if (!firefoxLiteVersionFinder.isCorrect()) {
            return;
        }

        result.versions.put(FIREFOX_LITE, firefoxLiteVersionFinder.getVersion());
        result.downloadUrl.put(FIREFOX_LITE, firefoxLiteVersionFinder.getDownloadUrl());
    }

    private static void checkFenix(AvailableApps result) {
        FenixVersionFinder fenixVersionFinder = FenixVersionFinder.create();
        if (!fenixVersionFinder.isCorrect()) {
            return;
        }

        result.versions.put(FENIX, fenixVersionFinder.getVersion());
        result.downloadUrl.put(FENIX, fenixVersionFinder.getDownloadUrl(DeviceABI.getAbi()));
    }


    /**
     * Internal method for retrieving the download url for FENNEC_RELEASE, FENNEC_BETA, FENNEC_NIGHTLY.
     * Reason: There are two methods for retrieving the url.
     * {@link MozillaFtp} works better but can fail. If it fails, then {@link OfficialApi} will be used.
     *
     * @param app
     * @param version
     * @return
     */
    private static String getDownloadUrlForFennec(App app, OfficialApi.Version version) {
        Optional<String> possibleUrl = MozillaFtp.getDownloadUrl(app, DeviceABI.getAbi(), version);
        if (possibleUrl.isPresent()) {
            return possibleUrl.get();
        }
        return OfficialApi.getDownloadUrl(app, DeviceABI.getAbi());
    }

    /**
     * Get the version name for the given app - if the app was in {@code appsToCheck} in the method
     * {@code create()}
     *
     * @param app
     * @return version name
     */
    Optional<String> getVersionName(App app) {
        return Optional.fromNullable(versions.get(app));
    }

    /**
     * Check if for a given app and a given installed version a newer version is available.
     *
     * @param app
     * @param installedVersion
     * @return is update available
     */
    public boolean isUpdateAvailable(App app, String installedVersion) {
        Optional<String> availableVersion = getVersionName(app);
        if (!availableVersion.isPresent()) {
            return false;
        }

        if (app == FENNEC_BETA) {
            String sanitizedAvailable = availableVersion.get().split("b")[0];
            return !sanitizedAvailable.contentEquals(installedVersion);
        }
        if (app == FIREFOX_LITE) {
            String sanitizedInstalled = installedVersion.split("\\(")[0];
            return !availableVersion.get().contentEquals(sanitizedInstalled);
        }

        return !availableVersion.get().contentEquals(installedVersion);
    }

    /**
     * Get the download url for the given app - if the app was in {@code appsToCheck} in the method
     * {@code create()}
     *
     * @param app
     * @return download url
     */
    Optional<String> getDownloadUrl(App app) {
        return Optional.fromNullable(downloadUrl.get(app));
    }

    /**
     * Should an app download triggered?
     *
     * @return
     */
    boolean isTriggerDownload() {
        return triggerDownload;
    }

    /**
     * Which app should be downloaded?
     *
     * @return
     */
    App getAppToDownload() {
        return appToDownload;
    }
}
