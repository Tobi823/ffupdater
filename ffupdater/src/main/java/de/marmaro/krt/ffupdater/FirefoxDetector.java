package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.github.dmstocking.optional.java.util.Optional;

import java.util.HashMap;
import java.util.Map;

/**
 * This class can detect the version number of the installed firefox.
 */
public class FirefoxDetector {
    private static final String RELEASE_PACKAGE_ID = "org.mozilla.firefox";
    private static final String BETA_PACKAGE_ID = "org.mozilla.firefox_beta";
    private static final String NIGHTLY_PACKAGE_ID = "org.mozilla.fennec_aurora";
    private static final String FOCUS_PACKAGE_ID = "org.mozilla.focus";
    private static final String KLAR_PACKAGE_ID = "org.mozilla.klar";

    private Map<UpdateChannel, Version> localVersions;

    private FirefoxDetector(Map<UpdateChannel, Version> localVersions) {
        this.localVersions = localVersions;
    }

    /**
     * Create an instance of FirefoxDetector (because an instance of PackageManager, only accessible
     * from {@link MainActivity}, is necessary)
     * @param packageManager
     * @return
     */
    public static FirefoxDetector create(PackageManager packageManager) {
        Map<UpdateChannel, Version> installed = new HashMap<>();

        Map<UpdateChannel, String> packageIds = new HashMap<>();
        packageIds.put(UpdateChannel.RELEASE, RELEASE_PACKAGE_ID);
        packageIds.put(UpdateChannel.BETA, BETA_PACKAGE_ID);
        packageIds.put(UpdateChannel.NIGHTLY, NIGHTLY_PACKAGE_ID);
        packageIds.put(UpdateChannel.FOCUS, FOCUS_PACKAGE_ID);
        packageIds.put(UpdateChannel.KLAR, KLAR_PACKAGE_ID);

        for (Map.Entry<UpdateChannel, String> packageId : packageIds.entrySet()) {
            Optional<Version> version = getVersion(packageId.getValue(), packageManager);
            if (version.isPresent()) {
                installed.put(packageId.getKey(), version.get());
            }
        }

        return new FirefoxDetector(installed);
    }

    /**
     * Get the version name and code for an app with the specific package name
     * @param packageName
     * @param packageManager
     * @return
     */
    private static Optional<Version> getVersion(String packageName, PackageManager packageManager) {
        try {
            PackageInfo releasePackage = packageManager.getPackageInfo(packageName, 0);
            Version version = new Version(releasePackage.versionName, releasePackage.versionCode);
            return Optional.of(version);
        } catch (PackageManager.NameNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * @return Return all versions of installed firefox apps.
     */
    public Map<UpdateChannel, Version> getLocalVersions() {
        return localVersions;
    }
}
