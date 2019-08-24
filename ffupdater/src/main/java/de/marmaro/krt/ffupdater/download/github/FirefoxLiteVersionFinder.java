package de.marmaro.krt.ffupdater.download.github;

import com.google.common.base.Optional;

/**
 * Created by Tobiwan on 22.08.2019.
 */
public class FirefoxLiteVersionFinder {

    private static final String OWNER = "mozilla-tw";
    private static final String REPOSITORY = "FirefoxLite";

    private String version;
    private String downloadUrl;
    private boolean faulty;

    private FirefoxLiteVersionFinder() {

    }

    public static FirefoxLiteVersionFinder create() {
        FirefoxLiteVersionFinder newObject = new FirefoxLiteVersionFinder();
        Optional<LatestReleaseSearcher.Release> latestRelease = LatestReleaseSearcher.findLatestRelease(OWNER, REPOSITORY);
        if (!latestRelease.isPresent()) {
            newObject.faulty = true;
            return newObject;
        }

        newObject.version = latestRelease.get().getTagName().replace("v", "");
        newObject.downloadUrl = latestRelease.get().getAssets().get(0).getDownloadUrl();
        return newObject;
    }

    public boolean isFaulty() {
        return faulty;
    }

    public String getVersion() {
        if (faulty) {
            throw new IllegalArgumentException("FirefoxLiteVersionFinder is faulty");
        }
        return version;
    }

    public String getDownloadUrl() {
        if (faulty) {
            throw new IllegalArgumentException("FirefoxLiteVersionFinder is faulty");
        }
        return downloadUrl;
    }
}
