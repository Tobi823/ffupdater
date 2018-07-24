package de.marmaro.krt.ffupdater;

import org.apache.commons.lang.StringUtils;

import de.marmaro.krt.ffupdater.github.Asset;
import de.marmaro.krt.ffupdater.github.Release;

/**
 * This class builds the download url for every update channel (release, beta, nightly) depending on the
 * architecture (x86 vs ARM)
 */
public class DownloadUrl {
    public static final String PROPERTY_OS_ARCHITECTURE = "os.arch";

    public static final String RELEASE_URL = "https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi";
    public static final String RELEASE_X86_URL = "https://download.mozilla.org/?product=fennec-latest&os=android-x86&lang=multi";
    public static final String BETA_URL = "https://download.mozilla.org/?product=fennec-beta-latest&os=android&lang=multi";
    public static final String BETA_X86_URL = "https://download.mozilla.org/?product=fennec-beta-latest&os=android-x86&lang=multi";
    public static final String NIGHTLY_URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-%s.multi.android-arm.apk";
    public static final String NIGHTLY_X86_URL_TEMPLATE = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-%s.multi.android-i386.apk";

    private boolean isX86Architecture;
    private ApiResponses apiResponses;

    public DownloadUrl(boolean isX86Architecture, ApiResponses apiResponses) {
        this.isX86Architecture = isX86Architecture;
        this.apiResponses = apiResponses;
    }

    /**
     * @param apiResponses must be non-null for generating the download url for nightly
     * @return create a new {@link DownloadUrl} object and returns it.
     */
    public static DownloadUrl create(ApiResponses apiResponses) {
        String osArch = System.getProperty(PROPERTY_OS_ARCHITECTURE);
        boolean isX86 = "i686".equals(osArch) || "x86_64".equals(osArch);
        return new DownloadUrl(isX86, apiResponses);
    }

    /**
     * @return create a new {@link DownloadUrl} object which only can generate the download url for
     * release and beta.
     */
    public static DownloadUrl create() {
        return create(null);
    }

    /**
     * Update the internal cache of {@link ApiResponses}. If the parameter is not null, than
     * the download URL for nightly can (again) be generated.
     * @param apiResponses
     */
    public void update(ApiResponses apiResponses) {
        this.apiResponses = apiResponses;
    }

    /**
     * Generate the download url for the specific {@link UpdateChannel}
     * @param updateChannel
     * @return
     */
    public String getUrl(UpdateChannel updateChannel) {
        switch (updateChannel) {
            case RELEASE:
                return isX86Architecture ? RELEASE_X86_URL : RELEASE_URL;
            case BETA:
                return isX86Architecture ? BETA_X86_URL : BETA_URL;
            case NIGHTLY:
                String template = isX86Architecture ? NIGHTLY_X86_URL_TEMPLATE : NIGHTLY_URL_TEMPLATE;
                String nightlyVersion = apiResponses.getMozillaApiResponse().getNightlyVersion();
                return String.format(template, nightlyVersion);
            case FOCUS:
            case KLAR:
                return getAssetFor(apiResponses.getGithubApiResponse(), updateChannel.getName()).getBrowserDownloadUrl();
            default:
                throw new IllegalArgumentException("An unknown UpdateChannel exists. Please add this new enum value to this switch-statement");
        }
    }

    /**
     * Check if the download url can be generated for the given {@link UpdateChannel}
     * @param updateChannel
     * @return
     */
    public boolean isUrlAvailable(UpdateChannel updateChannel) {
        switch (updateChannel) {
            case RELEASE:
            case BETA:
                return true;
            case NIGHTLY:
                return apiResponses != null &&
                        apiResponses.getMozillaApiResponse() != null &&
                        apiResponses.getMozillaApiResponse().getNightlyVersion() != null;
            case FOCUS:
            case KLAR:
                return apiResponses != null &&
                        apiResponses.getGithubApiResponse() != null &&
                        getAssetFor(apiResponses.getGithubApiResponse(), updateChannel.getName()) != null;
            default:
                throw new IllegalArgumentException("An unknown UpdateChannel exists. Please add this new enum value to this switch-statement");
        }
    }

    private static Asset getAssetFor(Release release, String name) {
        for (Asset asset : release.getAssets()) {
            if (null != asset && StringUtils.containsIgnoreCase(asset.getName(), name)) {
                return asset;
            }
        }

        return null;
    }
}
