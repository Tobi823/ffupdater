package de.marmaro.krt.ffupdater.download;

import com.google.common.base.Optional;

/**
 * Access the version name and the download url for Firefox Lite from Github.
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
        Optional<GithubReleaseParser.Release> latestRelease = GithubReleaseParser.findLatestRelease(OWNER, REPOSITORY);
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
