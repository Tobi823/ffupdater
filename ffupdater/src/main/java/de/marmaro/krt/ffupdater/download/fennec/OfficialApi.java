package de.marmaro.krt.ffupdater.download.fennec;

/**
 * Get the download link for the latest Fennec release (release, beta, nightly) from the official
 * Mozilla API. This API only supports
 */
class OfficialApi {
    private static final String DEFAULT_OS = "android";
    private static final String X86_OS = "android-x86";
    private static final String RELEASE_PRODUCT = "fennec-latest";
    private static final String BETA_PRODUCT = "fennec-beta-latest";
    private static final String NIGHTLY_PRODUCT = "fennec-nightly-latest";

    private static final String URL_TEMPLATE = "https://download.mozilla.org/?product=%s&os=%s&lang=multi";

    static String getDownloadUrl(UpdateChannel updateChannel, Platform platform) {
        String operatingSystem = getOperatingSystem(platform);
        String product = getProduct(updateChannel);
        return String.format(URL_TEMPLATE, product, operatingSystem);
    }

    private static String getOperatingSystem(Platform platform) {
        switch (platform) {
            case AARCH64:
            case ARM:
                return DEFAULT_OS;
            case X86:
            case X86_64:
                return X86_OS;
        }
        throw new IllegalArgumentException("unsupported platform " + platform);
    }

    private static String getProduct(UpdateChannel updateChannel) {
        switch (updateChannel) {
            case RELEASE:
                return RELEASE_PRODUCT;
            case BETA:
                return BETA_PRODUCT;
            case NIGHTLY:
                return NIGHTLY_PRODUCT;
        }
        throw new IllegalArgumentException("unsupported update channel " + updateChannel);
    }
}
