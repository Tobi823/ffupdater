package de.marmaro.krt.ffupdater.version;

import androidx.annotation.Nullable;

/**
 * Access the version name and the download url for Firefox Lite from Github.
 * https://github.com/mozilla-tw/FirefoxLite/releases
 * https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest
 */
class FirefoxLite {
    private static final String OWNER = "mozilla-tw";
    private static final String REPOSITORY = "FirefoxLite";

    private String version;
    private String downloadUrl;

    private FirefoxLite() {
    }

    @Nullable
    static FirefoxLite findLatest() {
        FirefoxLite newObject = new FirefoxLite();
        GithubConsumer.Release latestRelease = GithubConsumer.findLatestRelease(OWNER, REPOSITORY);
        if (latestRelease == null) {
            return null;
        }

        newObject.version = latestRelease.getTagName().replace("v", "");
        newObject.downloadUrl = latestRelease.getAssets().get(0).getDownloadUrl();
        return newObject;
    }

    String getVersion() {
        return version;
    }

    String getDownloadUrl() {
        return downloadUrl;
    }
}
