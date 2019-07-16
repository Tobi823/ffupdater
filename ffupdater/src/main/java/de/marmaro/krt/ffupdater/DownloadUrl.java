package de.marmaro.krt.ffupdater;

import static android.os.Build.CPU_ABI;
import static android.os.Build.CPU_ABI2;
import static android.os.Build.SUPPORTED_ABIS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * This class builds (depending on the architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadUrl {
    private static final String DEFAULT_OS = "android";
    private static final String X86_OS = "android-x86";
    private static final String RELEASE_PRODUCT = "fennec-latest";
    private static final String BETA_PRODUCT = "fennec-beta-latest";
    private static final String NIGHTLY_PRODUCT = "fennec-nightly-latest";

    private static final String URL_TEMPLATE = "https://download.mozilla.org/?product=%s&os=%s&lang=multi";
    private static final String X86_ARCH = "x86";
    private static final String X64_ARCH = "x86_64";

    /**
     * Return the URL for the given operating system and update channel
     * Update URI as specified in https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
     *
     * @param updateChannel which fennec browser should be used? (released, beta or nightly)
     * @return url for downloading the fennec browser
     */
    public static String getUrl(String updateChannel) {
        return String.format(URL_TEMPLATE, getProduct(updateChannel), getOperatingSystem());
    }

    private static String getOperatingSystem() {
        String[] supportedAbis;
        if (SDK_INT < LOLLIPOP) {
            supportedAbis = new String[] {CPU_ABI, CPU_ABI2};
        } else {
            supportedAbis = SUPPORTED_ABIS;
        }

        for (String supportedAbi : supportedAbis) {
            switch (supportedAbi) {
                case "armeabi-v7a":
                    return DEFAULT_OS;
                case "x86":
                    return X86_OS;
            }
        }
        return DEFAULT_OS;
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
