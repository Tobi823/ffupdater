package de.marmaro.krt.ffupdater;

import com.google.common.base.Preconditions;

import java.security.InvalidParameterException;

/**
 * This class builds (depending on the architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadUrl {
    public static final String NON_X86_ARCHITECTURE = "android";
    public static final String X86_ARCHITECTURE = "android-x86";
    public static final String RELEASE_PRODUCT = "fennec-latest";
    public static final String BETA_PRODUCT = "fennec-beta-latest";
    public static final String NIGHTLY_PRODUCT = "fennec-nightly-latest";

    private static final String URL_TEMPLATE = "https://download.mozilla.org/?product=%s&os=%s&lang=multi";

    private String architecture;
    private int apiLevel;

    public DownloadUrl(String architecture, int apiLevel) {
        Preconditions.checkNotNull(architecture, "architecture must not be null");
        this.architecture = architecture;
        this.apiLevel = apiLevel;
    }

    public String getArchitecture() {
        return architecture;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    /**
     * Return the URL for the given architecture and update channel
     * Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
     * @param updateChannel
     * @return
     */
    public String getUrl(String updateChannel) {
        String product = getProduct(updateChannel);
        String architecture = getMozillaApiArchitecture();
        return String.format(URL_TEMPLATE, product, architecture);
    }

    private String getMozillaApiArchitecture() {
        if (architecture.equals("i686") || architecture.equals("x86_64")) {
            return X86_ARCHITECTURE;
        }

        return NON_X86_ARCHITECTURE;
    }

    private String getProduct(String updateChannel) {
        switch (updateChannel) {
            case "version":
                return RELEASE_PRODUCT;
            case "beta_version":
                return BETA_PRODUCT;
            case "nightly_version":
                return NIGHTLY_PRODUCT;
            default:
                throw new IllegalArgumentException("Unknown update channel");
        }
    }
}
