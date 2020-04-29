package de.marmaro.krt.ffupdater.version;

/**
 * Access the version name and the download url for Firefox Lite from Github.
 */
class FirefoxLite {
    private static final String OWNER = "mozilla-tw";
    private static final String REPOSITORY = "FirefoxLite";

    private String version;
    private String downloadUrl;
    private boolean correct = true;

    private FirefoxLite() {
    }

    static FirefoxLite findLatest() {
        FirefoxLite newObject = new FirefoxLite();
        GithubReleaseParser.Release latestRelease = GithubReleaseParser.findLatestRelease(OWNER, REPOSITORY);
        if (latestRelease == null) {
            newObject.correct = false;
            return newObject;
        }

        newObject.version = latestRelease.getTagName().replace("v", "");
        newObject.downloadUrl = latestRelease.getAssets().get(0).getDownloadUrl();
        return newObject;
    }

    boolean isCorrect() {
        return correct;
    }

    String getVersion() {
        return version;
    }

    String getDownloadUrl() {
        return downloadUrl;
    }
}
