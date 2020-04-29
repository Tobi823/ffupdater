package de.marmaro.krt.ffupdater.version;

/**
 * Access the version name and the download url for Firefox Lite from Github.
 */
class FirefoxLite {
    private static final String OWNER = "mozilla-tw";
    private static final String REPOSITORY = "FirefoxLite";

    private String version;
    private String downloadUrl;

    private FirefoxLite() {
    }

    static FirefoxLite findLatest() {
        FirefoxLite newObject = new FirefoxLite();
        GithubReleaseParser.Release latestRelease = GithubReleaseParser.findLatestRelease(OWNER, REPOSITORY);
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
