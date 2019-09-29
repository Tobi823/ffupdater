package de.marmaro.krt.ffupdater.download.github;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.LocalDevice;

import static de.marmaro.krt.ffupdater.LocalDevice.PlatformX86orArm.ARM;
import static de.marmaro.krt.ffupdater.LocalDevice.PlatformX86orArm.X86;

/**
 * Created by Tobiwan on 22.08.2019.
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
        Optional<LatestReleaseSearcher.Release> latestRelease = LatestReleaseSearcher.findLatestRelease(OWNER, REPOSITORY);
        if (!latestRelease.isPresent()) {
            newObject.correct = false;
            return newObject;
        }

        newObject.version = latestRelease.get().getTagName().replace("v", "");
        for (LatestReleaseSearcher.Asset asset : latestRelease.get().getAssets()) {
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

    public String getDownloadUrl(App app, LocalDevice.PlatformX86orArm platform) {
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

            if (platform == ARM && !nameLowerCase.contains("arm")) {
                continue;
            }

            if (platform == X86 && !nameLowerCase.contains("x86")) {
                continue;
            }

            return downloadUrls.get(name);
        }
        throw new IllegalArgumentException("Missing map entry");
    }
}
