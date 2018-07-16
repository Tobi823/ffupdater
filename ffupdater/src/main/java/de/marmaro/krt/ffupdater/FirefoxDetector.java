package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class can detect the version number of the installed firefox.
 */
public class FirefoxDetector {
    private static final String RELEASE_PACKAGE_ID = "org.mozilla.firefox";
    private static final String BETA_PACKAGE_ID = "org.mozilla.firefox_beta";
    private static final String NIGHTLY_PACKAGE_ID = "org.mozilla.fennec_aurora";

    private LocalInstalledVersions localVersions;

    private FirefoxDetector(LocalInstalledVersions localVersions) {
        this.localVersions = localVersions;
    }

    /**
     * Create an instance of FirefoxDetector (because an instance of PackageManager, only accessible
     * from {@link MainActivity}, is necessary)
     * @param packageManager
     * @return
     */
    public static FirefoxDetector create(PackageManager packageManager) {
        LocalInstalledVersions localVersions = new LocalInstalledVersions();

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

        return new FirefoxDetector(localVersions);
    }

    /**
     * Get the version name and code for an app with the specific package name
     * @param packageName
     * @param packageManager
     * @return
     */
    @Nullable
    private static Version getVersion(String packageName, PackageManager packageManager) {
        try {
            PackageInfo releasePackage = packageManager.getPackageInfo(packageName, 0);
            return new Version(releasePackage.versionName, releasePackage.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * @return Return all versions of installed firefox apps.
     */
    public LocalInstalledVersions getLocalVersions() {
        return localVersions;
    }
}
