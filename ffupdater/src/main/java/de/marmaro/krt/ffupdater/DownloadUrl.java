package de.marmaro.krt.ffupdater;

import java.security.InvalidParameterException;

/**
 * This class builds (depending on the architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadUrl {
    public static final String NON_X86_ARCHITECTURE = "android";
    public static final String X86_ARCHITECTURE = "android-x86";

    private static final String URL_TEMPLATE = "https://download.mozilla.org/?product=fennec-latest&os=%s&lang=multi";
    private static final String BETA_URL_TEMPLAYE = "https://download.mozilla.org/?product=fennec-beta-latest&os=%s&lang=multi";

    //NIGHTLY BUILD HAS DIFFERENT URL
    private static final String NIGHTLY_URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-%s.multi.android-arm.apk";
    private static final String NIGHTLY_X86_URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-%s.multi.android-i386.apk";

    private String architecture;
    private int apiLevel;

    public DownloadUrl(String architecture, int apiLevel) {
        this.architecture = architecture;
        this.apiLevel = apiLevel;
    }

    public String getArchitecture() {
        return architecture;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public String getUrl(String updateChannel, Version version) {
        if (0 == apiLevel || null == architecture) {
            throw new InvalidParameterException("Please call setApiLevel and setArchitecture before calling getUrl()");
        }
        // INFO: Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
        String mozApiArch = getMozillaApiArchitecture();
        if ("version".contentEquals(updateChannel)) {
            return String.format(URL_TEMPLATE, mozApiArch);
        } else if ("beta_version".contentEquals(updateChannel)) {
            return String.format(BETA_URL_TEMPLAYE, mozApiArch);
        } else if ("nightly_version".contentEquals(updateChannel)) {
            if (mozApiArch.contentEquals(NON_X86_ARCHITECTURE)) {
                return String.format(NIGHTLY_URL_TEMPLATE, version.get());
            } else {
                return String.format(NIGHTLY_X86_URL_TEMPLATE, version.get());
            }
        }
        return String.format(URL_TEMPLATE, mozApiArch);

    }

    private String getMozillaApiArchitecture() {
        if (architecture.equals("i686") || architecture.equals("x86_64")) {
            return X86_ARCHITECTURE;
        }

        return NON_X86_ARCHITECTURE;
    }
}
