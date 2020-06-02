package de.marmaro.krt.ffupdater.version;

import androidx.annotation.Nullable;

/**
 * Access the version name and the download url for Firefox Lockwise from Github.
 * https://github.com/mozilla-lockwise/lockwise-android
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest
 */
class Lockwise {
    private static final String OWNER = "mozilla-lockwise";
    private static final String REPOSITORY = "lockwise-android";

    private String version;
    private String downloadUrl;

    private Lockwise() {
    }

    @Nullable
    static Lockwise findLatest() {
        Lockwise newObject = new Lockwise();
        GithubConsumer.Release latestRelease = GithubConsumer.findLatestRelease(OWNER, REPOSITORY);
        if (latestRelease == null) {
            return null;
        }

        String[] parts = latestRelease.getTagName().split("v");
        if (parts.length != 2) {
            return null;
        }
        newObject.version = parts[1].split("-")[0];
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
