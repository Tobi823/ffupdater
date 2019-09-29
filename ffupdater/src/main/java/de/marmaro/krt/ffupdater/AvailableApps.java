package de.marmaro.krt.ffupdater;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.marmaro.krt.ffupdater.download.fennec.FennecVersionFinder;
import de.marmaro.krt.ffupdater.download.fennec.MozillaFtp;
import de.marmaro.krt.ffupdater.download.fennec.OfficialApi;
import de.marmaro.krt.ffupdater.download.github.FenixVersionFinder;
import de.marmaro.krt.ffupdater.download.github.FirefoxLiteVersionFinder;
import de.marmaro.krt.ffupdater.download.github.FocusVersionFinder;

/**
 * Created by Tobiwan on 21.08.2019.
 */
public class AvailableApps {

    private Map<App, String> versions = new HashMap<>();
    private Map<App, String> downloadUrl = new HashMap<>();
    private boolean triggerDownload = false;
    private App appToDownload;

    private AvailableApps() {

    }

    static AvailableApps createAndTriggerDownload(Set<App> appsToCheck, App appToDownload) {
        HashSet<App> copiedAppsToCheck = new HashSet<>(appsToCheck);
        copiedAppsToCheck.add(appToDownload);

        AvailableApps availableApps = create(copiedAppsToCheck);
        availableApps.triggerDownload = true;
        availableApps.appToDownload = appToDownload;
        return availableApps;
    }

    public static AvailableApps create(Set<App> appsToCheck) {
        AvailableApps newObject = new AvailableApps();

        LocalDevice.Platform platform = LocalDevice.getPlatform();
        LocalDevice.PlatformX86orArm platformX86orArm = LocalDevice.getPlatformX86orArm();

        if (appsToCheck.contains(App.FENNEC_RELEASE) ||
                appsToCheck.contains(App.FENNEC_BETA) ||
                appsToCheck.contains(App.FENNEC_NIGHTLY)) {
            Optional<FennecVersionFinder.Response> response = FennecVersionFinder.getResponse();
            if (response.isPresent()) {
                if (appsToCheck.contains(App.FENNEC_RELEASE)) {
                    newObject.versions.put(App.FENNEC_RELEASE, response.get().getReleaseVersion());
                    newObject.downloadUrl.put(App.FENNEC_RELEASE, getDownloadUrlForFennec(App.FENNEC_RELEASE, platform, response.get()));
                }
                if (appsToCheck.contains(App.FENNEC_BETA)) {
                    newObject.versions.put(App.FENNEC_BETA, response.get().getBetaVersion());
                    newObject.downloadUrl.put(App.FENNEC_BETA, getDownloadUrlForFennec(App.FENNEC_BETA, platform, response.get()));
                }
                if (appsToCheck.contains(App.FENNEC_NIGHTLY)) {
                    newObject.versions.put(App.FENNEC_NIGHTLY, response.get().getNightlyVersion());
                    newObject.downloadUrl.put(App.FENNEC_NIGHTLY, getDownloadUrlForFennec(App.FENNEC_NIGHTLY, platform, response.get()));
                }
            }
        }

        if (appsToCheck.contains(App.FIREFOX_KLAR) ||
                appsToCheck.contains(App.FIREFOX_FOCUS)) {
            FocusVersionFinder focusVersionFinder = FocusVersionFinder.create();
            if (focusVersionFinder.isCorrect()) {
                newObject.versions.put(App.FIREFOX_FOCUS, focusVersionFinder.getVersion());
                newObject.downloadUrl.put(App.FIREFOX_FOCUS, focusVersionFinder.getDownloadUrl(App.FIREFOX_FOCUS, platformX86orArm));

                newObject.versions.put(App.FIREFOX_KLAR, focusVersionFinder.getVersion());
                newObject.downloadUrl.put(App.FIREFOX_KLAR, focusVersionFinder.getDownloadUrl(App.FIREFOX_KLAR, platformX86orArm));
            }
        }

        if (appsToCheck.contains(App.FIREFOX_LITE)) {
            FirefoxLiteVersionFinder firefoxLiteVersionFinder = FirefoxLiteVersionFinder.create();
            if (firefoxLiteVersionFinder.isCorrect()) {
                newObject.versions.put(App.FIREFOX_LITE, firefoxLiteVersionFinder.getVersion());
                newObject.downloadUrl.put(App.FIREFOX_LITE, firefoxLiteVersionFinder.getDownloadUrl());
            }
        }

        if (appsToCheck.contains(App.FENIX)) {
            FenixVersionFinder fenixVersionFinder = FenixVersionFinder.create();
            if (fenixVersionFinder.isCorrect()) {
                newObject.versions.put(App.FENIX, fenixVersionFinder.getVersion());
                newObject.downloadUrl.put(App.FENIX, fenixVersionFinder.getDownloadUrl(platform));
            }
        }
        return newObject;
    }

    private static String getDownloadUrlForFennec(App app, LocalDevice.Platform platform, FennecVersionFinder.Response response) {
        Optional<String> possibleUrl = MozillaFtp.getDownloadUrl(app, platform, response);
        if (possibleUrl.isPresent()) {
            return possibleUrl.get();
        }
        return OfficialApi.getDownloadUrl(app, platform);
    }

    Optional<String> findVersionName(App app) {
        return Optional.fromNullable(versions.get(app));
    }

    boolean isUpdateAvailable(App app, String installedVersion) {
        Optional<String> availableVersion = findVersionName(app);
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

    Optional<String> getDownloadUrl(App app) {
        return Optional.fromNullable(downloadUrl.get(app));
    }

    boolean isTriggerDownload() {
        return triggerDownload;
    }

    App getAppToDownload() {
        return appToDownload;
    }
}
