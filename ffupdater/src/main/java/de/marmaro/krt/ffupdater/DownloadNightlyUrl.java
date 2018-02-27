package de.marmaro.krt.ffupdater;

import java.security.InvalidParameterException;

/**
 * This class builds (depending on the architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadNightlyUrl {
    public static final String NON_X86_ARCHITECTURE = "android";
    public static final String X86_ARCHITECTURE = "android-x86";
    public static final String URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-60.0a1.multi.android-arm.apk";

    private String architecture;
    private int apiLevel;

    public DownloadNightlyUrl(String architecture, int apiLevel) {
        this.architecture = architecture;
        this.apiLevel = apiLevel;
    }

    public boolean isApiLevelSupported() {
        // https://support.mozilla.org/en-US/kb/will-firefox-work-my-mobile-device
        return (apiLevel >= 16);
    }

    public String getArchitecture() {
        return architecture;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public String getUrl() {
        if (0 == apiLevel || null == architecture) {
            throw new InvalidParameterException("Please call setApiLevel and setArchitecture before calling getUrl()");
        }

        // INFO: Unfortunately, there is only hardcoded URI in https://www.mozilla.org/en-US/firefox/android/nightly/all/. It should be changed in future.
        String mozApiArch = getMozillaApiArchitecture();
        return String.format(URL_TEMPLATE, mozApiArch);
    }

    private String getMozillaApiArchitecture() {
        if (architecture.equals("i686") || architecture.equals("x86_64")) {
            return X86_ARCHITECTURE;
        }

        return NON_X86_ARCHITECTURE;
    }
}
