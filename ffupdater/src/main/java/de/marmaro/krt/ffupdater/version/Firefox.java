package de.marmaro.krt.ffupdater.version;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

/**
 * Source url for Firefox Release, Firefox Beta and Firefox Nightly
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.fennec-production.latest
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.fennec-beta.latest
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 */
class Firefox {
    private static final String BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.%s.latest.%s";
    private static final String CHAIN_OF_TRUST_URL = BASE_URL + "/artifacts/public/chain-of-trust.json";
    private static final String DOWNLOAD_URL = BASE_URL + "/artifacts/public/build/%s.apk";

    private String timestamp;
    private String downloadUrl;

    private Firefox(String timestamp, String downloadUrl) {
        this.timestamp = timestamp;
        this.downloadUrl = downloadUrl;
    }

    /**
     * Do the network request to get the latest version name and download url for Fenix.
     *
     * @return result or null
     */
    static Firefox findLatest(App app, DeviceEnvironment.ABI abi) {
        final String namespaceSuffix;
        switch (app) {
            case FIREFOX_RELEASE:
                namespaceSuffix = "release";
                break;
            case FIREFOX_BETA:
                namespaceSuffix = "beta";
                break;
            case FIREFOX_NIGHTLY:
                namespaceSuffix = "nightly";
                break;
            default:
                throw new RuntimeException("unsupported app - must be FIREFOX_RELEASE, FIREFOX_BETA or FIREFOX_NIGHTLY");
        }
        final String abiAbbreviation;
        switch (abi) {
            case AARCH64:
                abiAbbreviation = "arm64-v8a";
                break;
            case ARM:
                abiAbbreviation = "armeabi-v7a";
                break;
            case X86:
                abiAbbreviation = "x86";
                break;
            case X86_64:
                abiAbbreviation = "x86_64";
                break;
            default:
                throw new RuntimeException("unsupported abi");
        }
        final String apkFile = abiAbbreviation + "/target";
        final String chainOfTrustUrl = String.format(CHAIN_OF_TRUST_URL, namespaceSuffix, abiAbbreviation);
        final String downloadUrl = String.format(DOWNLOAD_URL, namespaceSuffix, abiAbbreviation, apkFile);
        final String timestamp = MozillaCIConsumer.findLatest(chainOfTrustUrl).getTimestamp();
        return new Firefox(timestamp, downloadUrl);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
