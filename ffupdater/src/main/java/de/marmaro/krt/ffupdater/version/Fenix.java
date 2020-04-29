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
