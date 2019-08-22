package de.marmaro.krt.ffupdater;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

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

    private AvailableApps() {

    }

    public static AvailableApps create() {
        AvailableApps newObject = new AvailableApps();

        FennecVersionFinder.Response response = FennecVersionFinder.getResponse();
        newObject.versions.put(App.FENNEC_RELEASE, response.getReleaseVersion());
        newObject.versions.put(App.FENNEC_BETA, response.getBetaVersion());
        newObject.versions.put(App.FENNEC_NIGHTLY, response.getNightlyVersion());

        FocusVersionFinder focusVersionFinder = FocusVersionFinder.create();
        newObject.versions.put(App.FIREFOX_FOCUS, focusVersionFinder.getVersion());
        newObject.versions.put(App.FIREFOX_KLAR, focusVersionFinder.getVersion());

        FirefoxLiteVersionFinder firefoxLiteVersionFinder = FirefoxLiteVersionFinder.create();
        newObject.versions.put(App.FIREFOX_LITE, firefoxLiteVersionFinder.getVersion());

        FenixVersionFinder fenixVersionFinder = FenixVersionFinder.create();
        newObject.versions.put(App.FENIX, fenixVersionFinder.getVersion());

        // ====

        LocalDevice.Platform platform = LocalDevice.getPlatform();
        newObject.downloadUrl.put(App.FENNEC_RELEASE, getDownloadUrlForFennec(App.FENNEC_RELEASE, platform, response));
        newObject.downloadUrl.put(App.FENNEC_BETA, getDownloadUrlForFennec(App.FENNEC_BETA, platform, response));
        newObject.downloadUrl.put(App.FENNEC_NIGHTLY, getDownloadUrlForFennec(App.FENNEC_NIGHTLY, platform, response));

        LocalDevice.PlatformX86orArm platformX86orArm = LocalDevice.getPlatformX86orArm();
        newObject.downloadUrl.put(App.FIREFOX_FOCUS, focusVersionFinder.getDownloadUrl(App.FIREFOX_FOCUS, platformX86orArm));
        newObject.downloadUrl.put(App.FIREFOX_KLAR, focusVersionFinder.getDownloadUrl(App.FIREFOX_KLAR, platformX86orArm));

        newObject.downloadUrl.put(App.FIREFOX_LITE, firefoxLiteVersionFinder.getDownloadUrl());
        newObject.downloadUrl.put(App.FENIX, fenixVersionFinder.getDownloadUrl(platform));

        return newObject;
    }

    private static String getDownloadUrlForFennec(App app, LocalDevice.Platform platform, FennecVersionFinder.Response response) {
        Optional<String> possibleUrl = MozillaFtp.getDownloadUrl(app, platform, response);
        if (possibleUrl.isPresent()) {
            return possibleUrl.get();
        }
        return OfficialApi.getDownloadUrl(app, platform);
    }

    public String findVersionName(App app) {
        String versionName = versions.get(app);
        return versionName == null ? "" : versionName;
    }

    public boolean isUpdateAvailable(App app, String installedVersion) {
        if (app == App.FENNEC_BETA) {
            // TODO Extrabehandlung
        }
        return !findVersionName(app).contentEquals(installedVersion);
    }

    public String getDownloadUrl(App app) {
        String url = downloadUrl.get(app);
        return url == null ? "" : url;
    }
}
