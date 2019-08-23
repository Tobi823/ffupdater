package de.marmaro.krt.ffupdater.download.github;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.LocalDevice;

/**
 * Created by Tobiwan on 22.08.2019.
 */
public class FenixVersionFinder {

    public static final String OWNER = "mozilla-mobile";
    public static final String REPOSITORY = "fenix";

    private String version;
    private Map<String, String> downloadUrls = new HashMap<>();

    private FenixVersionFinder() {

    }

    public static FenixVersionFinder create() {
        LatestReleaseSearcher.Release latestRelease = LatestReleaseSearcher.findLatestRelease(OWNER, REPOSITORY);
        FenixVersionFinder newObject = new FenixVersionFinder();
        newObject.version = latestRelease.getTagName().replace("v", "");
        for (LatestReleaseSearcher.Asset asset : latestRelease.getAssets()) {
            newObject.downloadUrls.put(asset.getName(), asset.getDownloadUrl());
        }

        return newObject;
    }

    public String getVersion() {
        return version;
    }

    public String getDownloadUrl(LocalDevice.Platform platform) {
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
