package de.marmaro.krt.ffupdater.download;

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
    private boolean correct = true;

    private Focus() {
    }

    static Focus findLatest() {
        Focus newObject = new Focus();
        GithubReleaseParser.Release latestRelease = GithubReleaseParser.findLatestRelease(OWNER, REPOSITORY);
        if (latestRelease == null) {
            newObject.correct = false;
            return newObject;
        }

        newObject.version = latestRelease.getTagName().replace("v", "");
        for (GithubReleaseParser.Asset asset : latestRelease.getAssets()) {
            newObject.downloadUrls.put(asset.getName(), asset.getDownloadUrl());
        }
        return newObject;
    }

    boolean isCorrect() {
        return correct;
    }

    String getVersion() {
        return version;
    }

    String getDownloadUrl(App app, DeviceABI.ABI abi) {
        for (String name : downloadUrls.keySet()) {
            String nameLowerCase = name.toLowerCase();
            if (app == App.FIREFOX_FOCUS && !nameLowerCase.contains("focus")) {
                continue;
            }
            if (app == App.FIREFOX_KLAR && !nameLowerCase.contains("klar")) {
                continue;
            }
            if (abi == AARCH64 && !nameLowerCase.contains("arm64")) {
                continue;
            }
            if (abi == ARM && !nameLowerCase.contains("arm")) {
                continue;
            }
            return downloadUrls.get(name);
        }
        throw new IllegalArgumentException("Missing map entry");
    }
}
