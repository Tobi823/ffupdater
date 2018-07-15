package de.marmaro.krt.ffupdater;

import android.support.annotation.Nullable;

/**
 * This class builds (depending on the isX86Architecture) the download url.
 * Furthermore this class can validate if the api level is high enough for running firefox.
 */
public class DownloadUrl {
    public static final String PROPERTY_OS_ARCHITECTURE = "os.arch";

    public static final String RELEASE_URL = "https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi";
    public static final String RELEASE_X86_URL = "https://download.mozilla.org/?product=fennec-latest&os=android-x86&lang=multi";
    public static final String BETA_URL = "https://download.mozilla.org/?product=fennec-beta-latest&os=android&lang=multi";
    public static final String BETA_X86_URL = "https://download.mozilla.org/?product=fennec-beta-latest&os=android-x86&lang=multi";
    public static final String NIGHTLY_URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-%s.multi.android-arm.apk";
    public static final String NIGHTLY_X86_URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-%s.multi.android-arm.apk";

    private boolean isX86Architecture;
    private MobileVersions mobileVersions;

    private DownloadUrl(boolean isX86Architecture, MobileVersions mobileVersions) {
        this.isX86Architecture = isX86Architecture;
        this.mobileVersions = mobileVersions;
    }

    public static DownloadUrl create(MobileVersions mobileVersions) {
        String osArch = System.getProperty(PROPERTY_OS_ARCHITECTURE);
        return new DownloadUrl("i686".equals(osArch) || "x86_64".equals(osArch), mobileVersions);
    }

    public static DownloadUrl create() {
        return create(null);
    }

    public void update(MobileVersions mobileVersions) {
        this.mobileVersions = mobileVersions;
    }

    public String getUrl(UpdateChannel updateChannel) {
        switch (updateChannel) {
            case RELEASE:
                return isX86Architecture ? RELEASE_X86_URL : RELEASE_URL;
            case BETA:
                return isX86Architecture ? BETA_X86_URL : BETA_URL;
            case NIGHTLY:
                String template = isX86Architecture ? NIGHTLY_X86_URL_TEMPLATE : NIGHTLY_URL_TEMPLATE;
                return String.format(template, mobileVersions.getValueBy(UpdateChannel.NIGHTLY));
            default:
                throw new IllegalArgumentException("An unknown UpdateChannel exists. Please add this new enum value to this switch-statement");
        }
    }

    public boolean isUrlAvailable(UpdateChannel updateChannel) {
        return updateChannel != UpdateChannel.NIGHTLY || mobileVersions != null;
    }
}
