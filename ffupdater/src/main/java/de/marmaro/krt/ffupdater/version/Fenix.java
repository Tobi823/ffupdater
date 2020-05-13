package de.marmaro.krt.ffupdater.version;

import androidx.annotation.Nullable;

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
class Fenix {
    private static final String OWNER = "mozilla-mobile";
    private static final String REPOSITORY = "fenix";

    private String version;
    private final Map<String, String> downloadUrls = new HashMap<>();

    private Fenix() {
    }

    /**
     * Do the network request to get the latest version name and download url for Fenix.
     *
     * @return result or null
     */
    @Nullable
    static Fenix findLatest() {
        Fenix newObject = new Fenix();
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

    String getDownloadUrl(DeviceABI.ABI abi) {
        for (String name : downloadUrls.keySet()) {
            if (isValidAbi(abi, name)) {
                return downloadUrls.get(name);
            }
        }
        throw new IllegalArgumentException("missing download url for " + abi);
    }

    private boolean isValidAbi(DeviceABI.ABI abi, String name) {
        String nameLowerCase = name.toLowerCase();
        switch (abi) {
            case AARCH64:
                return nameLowerCase.contains("arm64");
            case ARM:
                return nameLowerCase.contains("arm") && !nameLowerCase.contains("arm64");
            case X86_64:
                return nameLowerCase.contains("x86_64");
            case X86:
                return nameLowerCase.contains("x86") && !nameLowerCase.contains("x86_64");
            default:
                throw new IllegalArgumentException("invalid abi");
        }
    }
}
