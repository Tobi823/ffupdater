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
    private boolean correct = true;

    private FirefoxLiteVersionFinder() {

    }

    public static FirefoxLiteVersionFinder create() {
        FirefoxLiteVersionFinder newObject = new FirefoxLiteVersionFinder();
        Optional<LatestReleaseSearcher.Release> latestRelease = LatestReleaseSearcher.findLatestRelease(OWNER, REPOSITORY);
        if (!latestRelease.isPresent()) {
            newObject.correct = false;
            return newObject;
        }

        newObject.version = latestRelease.get().getTagName().replace("v", "");
        newObject.downloadUrl = latestRelease.get().getAssets().get(0).getDownloadUrl();
        return newObject;
    }

    public boolean isCorrect() {
        return correct;
    }

    public String getVersion() {
        if (!correct) {
            throw new IllegalArgumentException("FirefoxLiteVersionFinder is faulty");
        }
        return version;
    }

    public String getDownloadUrl() {
        if (!correct) {
            throw new IllegalArgumentException("FirefoxLiteVersionFinder is faulty");
        }
        return downloadUrl;
    }
}
