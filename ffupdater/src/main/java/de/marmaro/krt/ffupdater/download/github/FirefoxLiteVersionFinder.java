package de.marmaro.krt.ffupdater.download.github;

/**
 * Created by Tobiwan on 22.08.2019.
 */
public class FirefoxLiteVersionFinder {

    private static final String OWNER = "mozilla-tw";
    private static final String REPOSITORY = "FirefoxLite";

    private String version;
    private String downloadUrl;

    private FirefoxLiteVersionFinder() {

    }

    public static FirefoxLiteVersionFinder create() {
        LatestRelease.Release latestRelease = LatestRelease.findLatestRelease(OWNER, REPOSITORY);
        FirefoxLiteVersionFinder newObject = new FirefoxLiteVersionFinder();
        newObject.version = latestRelease.getTagName().replace("v", "");
        newObject.downloadUrl = latestRelease.getAssets().get(0).getDownloadUrl();
        return newObject;
    }

    public String getVersion() {
        return version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
