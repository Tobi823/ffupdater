package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * This class can access the version number of the installed firefox.
 */
public class FirefoxMetadata {
    private static final String PACKAGE_ID = "org.mozilla.firefox";
    private static final String PACKAGE_ID_BETA = "org.mozilla.firefox_beta";
    private static final String PACKAGE_ID_NIGHTLY = "org.mozilla.fennec_aurora";

    private boolean installed;
    private PackageInfo packageInfo;

    private FirefoxMetadata(boolean installed, PackageInfo packageInfo) {
        this.installed = installed;
        this.packageInfo = packageInfo;
    }

    public static FirefoxMetadata create(PackageManager packageManager) {
        try {
            switch (UpdateChannel.channel) {
                case "beta_version":
                    return new FirefoxMetadata(true, packageManager.getPackageInfo(PACKAGE_ID_BETA, 0));
                case "nightly_version":
                    return new FirefoxMetadata(true, packageManager.getPackageInfo(PACKAGE_ID_NIGHTLY, 0));
                default:
                    return new FirefoxMetadata(true, packageManager.getPackageInfo(PACKAGE_ID, 0));
            }
        } catch (PackageManager.NameNotFoundException e) {
            //package not found -> firefox is not installed
        }
        return new FirefoxMetadata(false, null);
    }


    /**
     * @return is the firefox installed on the android smartphone?
     */
    boolean isInstalled() {
        return installed;
    }

    /**
     * @return if firefox is installed, return the versionCode. if firefox is not installed, return 0.
     */
    int getVersionCode() {
        if (installed) {
            return packageInfo.versionCode;
        }
        return 0;

    }

    /**
     * @return if firefox is installed, return the versionName. if firefox is not installed, return an empty string.
     */
    String getVersionName() {
        if (installed) {
            return packageInfo.versionName;
        }
        return "";
    }

    /**
     * @return if firefox is installed, return a Version object (containing the versionName).
     * @throws IllegalArgumentException when firefox is not installed
     */
    public Version getVersion() {
        String versionName = getVersionName();
        return new Version(versionName);
    }
}
