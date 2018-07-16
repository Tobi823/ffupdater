package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class can access the version number of the installed firefox.
 */
public class FirefoxMetadata {
    public static final String RELEASE_PACKAGE_ID = "org.mozilla.firefox";
    public static final String BETA_PACKAGE_ID = "org.mozilla.firefox_beta";
    public static final String NIGHTLY_PACKAGE_ID = "org.mozilla.fennec_aurora";

    private LocalVersions localVersions;

    private FirefoxMetadata(LocalVersions localVersions) {
        this.localVersions = localVersions;
    }

    public static FirefoxMetadata create(PackageManager packageManager) {
        LocalVersions localVersions = new LocalVersions();

        Map<UpdateChannel, String> mapping = new HashMap<>();
        mapping.put(UpdateChannel.RELEASE, RELEASE_PACKAGE_ID);
        mapping.put(UpdateChannel.BETA, BETA_PACKAGE_ID);
        mapping.put(UpdateChannel.NIGHTLY, NIGHTLY_PACKAGE_ID);

        for (Map.Entry<UpdateChannel, String> entry : mapping.entrySet()) {
            Version version = getVersion(entry.getValue(), packageManager);
            if (null != version) {
                localVersions.setVersion(entry.getKey(), version);
            }
        }

        return new FirefoxMetadata(localVersions);
    }

    @Nullable
    private static Version getVersion(String packageName, PackageManager packageManager) {
        try {
            PackageInfo releasePackage = packageManager.getPackageInfo(packageName, 0);
            return new Version(releasePackage.versionName, releasePackage.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public LocalVersions getLocalVersions() {
        return localVersions;
    }
}
