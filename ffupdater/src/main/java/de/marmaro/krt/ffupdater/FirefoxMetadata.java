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
    private Browser browser;

    private FirefoxMetadata(boolean installed, PackageInfo packageInfo, Browser browser) {
        this.installed = installed;
        this.packageInfo = packageInfo;
        this.browser = browser;
    }

    public static FirefoxMetadata create(PackageManager packageManager) {
        return create(packageManager, UpdateChannel.channel);
    }

    public static FirefoxMetadata create(PackageManager packageManager, String updateChannel) {
        try {
            switch (updateChannel) {
                case "beta_version":
                    return new FirefoxMetadata(true, packageManager.getPackageInfo(PACKAGE_ID_BETA, 0), Browser.FENNEC_BETA);
                case "nightly_version":
                    return new FirefoxMetadata(true, packageManager.getPackageInfo(PACKAGE_ID_NIGHTLY, 0), Browser.FENNEC_NIGHTLY);
                default:
                    return new FirefoxMetadata(true, packageManager.getPackageInfo(PACKAGE_ID, 0), Browser.FENNEC_RELEASE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            //package not found -> firefox is not installed
        }
        return new FirefoxMetadata(false, null, Browser.FENNEC_RELEASE);
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
        return new Version(versionName, browser);
    }
}
