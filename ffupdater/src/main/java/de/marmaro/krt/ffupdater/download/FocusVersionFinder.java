package de.marmaro.krt.ffupdater.download;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.DeviceABI;

import static de.marmaro.krt.ffupdater.DeviceABI.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.DeviceABI.ABI.ARM;
import static de.marmaro.krt.ffupdater.DeviceABI.ABI.X86;
import static de.marmaro.krt.ffupdater.DeviceABI.ABI.X86_64;

/**
 * Access the version name and the download url for Firefox Focus and Firefox Klar from Github.
 */
public class FocusVersionFinder {
    private static final String OWNER = "mozilla-mobile";
    private static final String REPOSITORY = "focus-android";

    private String version;
    private Map<String, String> downloadUrls = new HashMap<>();
    private boolean correct = true;

    private FocusVersionFinder() {
    }

    public static FocusVersionFinder create() {
        FocusVersionFinder newObject = new FocusVersionFinder();
        Optional<GithubReleaseParser.Release> latestRelease = GithubReleaseParser.findLatestRelease(OWNER, REPOSITORY);
        if (!latestRelease.isPresent()) {
            newObject.correct = false;
            return newObject;
        }

        newObject.version = latestRelease.get().getTagName().replace("v", "");
        for (GithubReleaseParser.Asset asset : latestRelease.get().getAssets()) {
            newObject.downloadUrls.put(asset.getName(), asset.getDownloadUrl());
        }
        return newObject;
    }

    public boolean isCorrect() {
        return correct;
    }

    public String getVersion() {
        if (!correct) {
            throw new IllegalArgumentException("FocusVersionFinder is correct");
        }
        return version;
    }

    public String getDownloadUrl(App app, DeviceABI.ABI abi) {
        if (!correct) {
            throw new IllegalArgumentException("FocusVersionFinder is correct");
        }
        for (String name : downloadUrls.keySet()) {
            String nameLowerCase = name.toLowerCase();

            if (app == App.FIREFOX_FOCUS && !nameLowerCase.contains("focus")) {
                continue;
            }

            if (app == App.FIREFOX_KLAR && !nameLowerCase.contains("klar")) {
                continue;
            }

            if ((abi == AARCH64 || abi == ARM) && !nameLowerCase.contains("arm")) {
                continue;
            }

            if ((abi == X86_64 || abi == X86) && !nameLowerCase.contains("x86")) {
                continue;
            }

            return downloadUrls.get(name);
        }
        throw new IllegalArgumentException("Missing map entry");
    }
}
