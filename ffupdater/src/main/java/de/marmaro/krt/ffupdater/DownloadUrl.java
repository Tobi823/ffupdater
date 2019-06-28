package de.marmaro.krt.ffupdater;

import com.google.common.base.Preconditions;

import java.security.InvalidParameterException;

/**
 * This class builds (depending on the architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadUrl {
    private static final String NON_X86_ARCHITECTURE = "android";
    private static final String X86_ARCHITECTURE = "android-x86";
    private static final String RELEASE_PRODUCT = "fennec-latest";
    private static final String BETA_PRODUCT = "fennec-beta-latest";
    private static final String NIGHTLY_PRODUCT = "fennec-nightly-latest";

    private static final String URL_TEMPLATE = "https://download.mozilla.org/?product=%s&os=%s&lang=multi";

    private static final String PROPERTY_OS_ARCHITECTURE = "os.arch";

    /**
     * Return the URL for the given architecture and update channel
     * Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
     *
     * @param updateChannel
     * @return
     */
    public static String getUrl(String updateChannel) {
        String product = getProduct(updateChannel);
        String architecture = getMozillaApiArchitecture();
        return String.format(URL_TEMPLATE, product, architecture);
    }

    private static String getMozillaApiArchitecture() {
        String architecture = System.getProperty(PROPERTY_OS_ARCHITECTURE);
        Preconditions.checkNotNull(architecture, "architecture must not be null");

        switch (architecture) {
            case "i686":
            case "x86_64":
                return X86_ARCHITECTURE;
            default:
                return NON_X86_ARCHITECTURE;
        }
    }

    private static String getProduct(String updateChannel) {
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
