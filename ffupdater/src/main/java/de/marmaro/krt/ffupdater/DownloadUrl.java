package de.marmaro.krt.ffupdater;

import java.security.InvalidParameterException;

/**
 * This class builds (depending on the architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadUrl {
    public static final String NON_X86_ARCHITECTURE = "android";
    public static final String X86_ARCHITECTURE = "android-x86";
    public static final String URL_TEMPLATE = "https://download.mozilla.org/?product=fennec-latest&os=%s&lang=multi";

    private String architecture;
    private int apiLevel;

    public DownloadUrl(String architecture, int apiLevel) {
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

        // INFO: Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
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
