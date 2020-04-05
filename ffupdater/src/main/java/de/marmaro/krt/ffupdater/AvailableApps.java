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
     * @param appsToCheck apps for update checks
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

        if (appsToCheck.contains(App.FENNEC_RELEASE) ||
                appsToCheck.contains(App.FENNEC_BETA) ||
                appsToCheck.contains(App.FENNEC_NIGHTLY)) {
            Optional<OfficialApi.Version> response = OfficialApi.getResponse();
            if (response.isPresent() && appsToCheck.contains(App.FENNEC_RELEASE)) {
                result.versions.put(App.FENNEC_RELEASE, response.get().getReleaseVersion());
                result.downloadUrl.put(App.FENNEC_RELEASE, getDownloadUrlForFennec(App.FENNEC_RELEASE, response.get()));
            }
            if (response.isPresent() && appsToCheck.contains(App.FENNEC_BETA)) {
                result.versions.put(App.FENNEC_BETA, response.get().getBetaVersion());
                result.downloadUrl.put(App.FENNEC_BETA, getDownloadUrlForFennec(App.FENNEC_BETA, response.get()));
            }
            if (response.isPresent() && appsToCheck.contains(App.FENNEC_NIGHTLY)) {
                result.versions.put(App.FENNEC_NIGHTLY, response.get().getNightlyVersion());
                result.downloadUrl.put(App.FENNEC_NIGHTLY, getDownloadUrlForFennec(App.FENNEC_NIGHTLY, response.get()));
            }
        }

        if (appsToCheck.contains(App.FIREFOX_KLAR) ||
                appsToCheck.contains(App.FIREFOX_FOCUS)) {
            FocusVersionFinder focusVersionFinder = FocusVersionFinder.create();
            if (focusVersionFinder.isCorrect()) {
                result.versions.put(App.FIREFOX_FOCUS, focusVersionFinder.getVersion());
                result.downloadUrl.put(App.FIREFOX_FOCUS, focusVersionFinder.getDownloadUrl(App.FIREFOX_FOCUS, DeviceABI.getAbi()));

                result.versions.put(App.FIREFOX_KLAR, focusVersionFinder.getVersion());
                result.downloadUrl.put(App.FIREFOX_KLAR, focusVersionFinder.getDownloadUrl(App.FIREFOX_KLAR, DeviceABI.getAbi()));
            }
        }

        if (appsToCheck.contains(App.FIREFOX_LITE)) {
            FirefoxLiteVersionFinder firefoxLiteVersionFinder = FirefoxLiteVersionFinder.create();
            if (firefoxLiteVersionFinder.isCorrect()) {
                result.versions.put(App.FIREFOX_LITE, firefoxLiteVersionFinder.getVersion());
                result.downloadUrl.put(App.FIREFOX_LITE, firefoxLiteVersionFinder.getDownloadUrl());
            }
        }

        if (appsToCheck.contains(App.FENIX)) {
            FenixVersionFinder fenixVersionFinder = FenixVersionFinder.create();
            if (fenixVersionFinder.isCorrect()) {
                result.versions.put(App.FENIX, fenixVersionFinder.getVersion());
                result.downloadUrl.put(App.FENIX, fenixVersionFinder.getDownloadUrl(DeviceABI.getAbi()));
            }
        }
        return result;
    }

    /**
     * Internal method for retrieving the download url for FENNEC_RELEASE, FENNEC_BETA, FENNEC_NIGHTLY.
     * Reason: There are two methods for retrieving the url.
     * {@link MozillaFtp} works better but can fail. If it fails, then {@link OfficialApi} will be used.
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
     * @param app
     * @return version name
     */
    Optional<String> getVersionName(App app) {
        return Optional.fromNullable(versions.get(app));
    }

    /**
     * Check if for a given app and a given installed version a newer version is available.
     * @param app
     * @param installedVersion
     * @return is update available
     */
    public boolean isUpdateAvailable(App app, String installedVersion) {
        Optional<String> availableVersion = getVersionName(app);
        if (!availableVersion.isPresent()) {
            return false;
        }

        if (app == App.FENNEC_BETA) {
            String sanitizedAvailable = availableVersion.get().split("b")[0];
            return !sanitizedAvailable.contentEquals(installedVersion);
        }
        if (app == App.FIREFOX_LITE) {
            String sanitizedInstalled = installedVersion.split("\\(")[0];
            return !availableVersion.get().contentEquals(sanitizedInstalled);
        }

        return !availableVersion.get().contentEquals(installedVersion);
    }

    /**
     * Get the download url for the given app - if the app was in {@code appsToCheck} in the method
     * {@code create()}
     * @param app
     * @return download url
     */
    Optional<String> getDownloadUrl(App app) {
        return Optional.fromNullable(downloadUrl.get(app));
    }

    /**
     * Should an app download triggered?
     * @return
     */
    boolean isTriggerDownload() {
        return triggerDownload;
    }

    /**
     * Which app should be downloaded?
     * @return
     */
    App getAppToDownload() {
        return appToDownload;
    }
}
