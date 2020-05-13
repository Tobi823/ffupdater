package de.marmaro.krt.ffupdater.version;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceABI;

import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.ARM;

/**
 * Access the version name and the download url for Firefox Focus and Firefox Klar from Github.
 */
class Focus {
    private static final String OWNER = "mozilla-mobile";
    private static final String REPOSITORY = "focus-android";

    private String version;
    private final Map<String, String> downloadUrls = new HashMap<>();

    private Focus() {
    }

    @Nullable
    static Focus findLatest() {
        Focus newObject = new Focus();
        GithubReleaseParser.Release latestRelease = GithubReleaseParser.findLatestRelease(OWNER, REPOSITORY);
        if (latestRelease == null) {
            return null;
        }

        newObject.version = latestRelease.getTagName().replace("v", "");
        for (GithubReleaseParser.Asset asset : latestRelease.getAssets()) {
            newObject.downloadUrls.put(asset.getName(), asset.getDownloadUrl());
        }
        return newObject;
    }

    String getVersion() {
        return version;
    }

    String getDownloadUrl(App app, DeviceABI.ABI abi) {
        for (String name : downloadUrls.keySet()) {
            if (isValidApp(app, name) && isValidAbi(abi, name)) {
                return downloadUrls.get(name);
            }
        }
        throw new IllegalArgumentException("missing download url for " + app + " and " + abi);
    }

    private boolean isValidApp(App app, String name) {
        String nameLowerCase = name.toLowerCase();
        switch (app) {
            case FIREFOX_FOCUS:
                return nameLowerCase.contains("focus");
            case FIREFOX_KLAR:
                return nameLowerCase.contains("klar");
            default:
                throw new IllegalArgumentException("invalid app");
        }
    }

    private boolean isValidAbi(DeviceABI.ABI abi, String name) {
        String nameLowerCase = name.toLowerCase();
        switch (abi) {
            case AARCH64:
                return nameLowerCase.contains("arm64");
            case ARM:
                return nameLowerCase.contains("arm") && !nameLowerCase.contains("arm64");
            default:
                throw new IllegalArgumentException("invalid abi");
        }
    }
}
