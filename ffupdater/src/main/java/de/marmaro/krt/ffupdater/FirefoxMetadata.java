package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * This class can access the version number of the installed firefox.
 */
public class FirefoxMetadata {
    public static final String PACKAGE_ID = "org.mozilla.firefox";

    private boolean installed;
	private PackageInfo packageInfo;

	private FirefoxMetadata(Builder builder) {
		this.installed = builder.installed;
		this.packageInfo = builder.packageInfo;
	}

	/**
     * @return is the firefox installed on the android smartphone?
     */
    public boolean isInstalled() {
        return installed;
    }

    /**
     * @return if firefox is installed, return the versionCode. if firefox is not installed, return 0.
     */
    public int getVersionCode() {
        if (null == packageInfo) {
            return 0;
        }

        return packageInfo.versionCode;
    }

    /**
     * @return if firefox is installed, return the versionName. if firefox is not installed, return an empty string.
     */
    public String getVersionName() {
        if (null == packageInfo) {
            return "";
        }

        return packageInfo.versionName;
    }

    /**
     * @return if firefox is installed, return a Version object (containing the versionName).
     * @throws IllegalArgumentException when firefox is not installed
     */
    public Version getVersion() {
        String versionName = getVersionName();
        return new Version(versionName);
    }

    // this class can only be build with the method checkLocalInstalledFirefox from this Builder
	public static class Builder {
		private boolean installed;
		private PackageInfo packageInfo;

		public FirefoxMetadata checkLocalInstalledFirefox(PackageManager packageManager){
			try {
				packageInfo = packageManager.getPackageInfo(PACKAGE_ID, 0);
				installed = true;
			} catch (PackageManager.NameNotFoundException e) {
				installed = false;
			}
			return new FirefoxMetadata(this);
		}
	}
}
