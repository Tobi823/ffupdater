package de.marmaro.krt.ffupdater.download.github;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.LocalDevice;

/**
 * Created by Tobiwan on 22.08.2019.
 */
public class FenixVersionFinder {

    private static final String OWNER = "mozilla-mobile";
    private static final String REPOSITORY = "fenix";

    private String version;
    private Map<String, String> downloadUrls = new HashMap<>();
    private boolean correct = true;

    private FenixVersionFinder() {

    }

    public static FenixVersionFinder create() {
        FenixVersionFinder newObject = new FenixVersionFinder();
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
            throw new IllegalArgumentException("FenixVersionFinder is faulty");
        }
        return version;
    }

    public String getDownloadUrl(LocalDevice.Platform platform) {
        if (!correct) {
            throw new IllegalArgumentException("FenixVersionFinder is faulty");
        }
        for (String name : downloadUrls.keySet()) {
            String nameLowerCase = name.toLowerCase();

            if (platform == LocalDevice.Platform.AARCH64 && !nameLowerCase.contains("aarch64")) {
                continue;
            }

            if (platform == LocalDevice.Platform.ARM && !nameLowerCase.contains("arm")) {
                continue;
            }

            if (platform == LocalDevice.Platform.X86 && !nameLowerCase.contains("x86")) {
                continue;
            }

            if (platform == LocalDevice.Platform.X86_64 && !nameLowerCase.contains("x86_64")) {
                continue;
            }

            return downloadUrls.get(name);
        }
        throw new IllegalArgumentException("Missing map entry");
    }
}
