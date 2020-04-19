package de.marmaro.krt.ffupdater.download;

import java.util.HashMap;
import java.util.Map;

import de.marmaro.krt.ffupdater.device.DeviceABI;

import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.X86;
import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.X86_64;

/**
 * Access the version name and the download url for Fenix from Github.
 */
public class FenixVersionFinder {
    private static final String OWNER = "mozilla-mobile";
    private static final String REPOSITORY = "fenix";

    private String version;
    private final Map<String, String> downloadUrls = new HashMap<>();
    private boolean correct = true;

    private FenixVersionFinder() {
    }

    public static FenixVersionFinder create() {
        FenixVersionFinder newObject = new FenixVersionFinder();
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

    public boolean isCorrect() {
        return correct;
    }

    public String getVersion() {
        if (!correct) {
            throw new IllegalArgumentException("FenixVersionFinder is faulty");
        }
        return version;
    }

    public String getDownloadUrl(DeviceABI.ABI abi) {
        if (!correct) {
            throw new IllegalArgumentException("FenixVersionFinder is faulty");
        }
        for (String name : downloadUrls.keySet()) {
            String nameLowerCase = name.toLowerCase();

            if (abi == AARCH64 && !nameLowerCase.contains("aarch64")) {
                continue;
            }

            if (abi == ARM && !nameLowerCase.contains("arm")) {
                continue;
            }

            if (abi == X86 && !nameLowerCase.contains("x86")) {
                continue;
            }

            if (abi == X86_64 && !nameLowerCase.contains("x86_64")) {
                continue;
            }

            return downloadUrls.get(name);
        }
        throw new IllegalArgumentException("Missing map entry");
    }
}
