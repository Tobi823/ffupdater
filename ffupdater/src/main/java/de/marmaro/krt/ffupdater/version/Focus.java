package de.marmaro.krt.ffupdater.version;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

/**
 * Access the version name and the download url for Firefox Focus and Firefox Klar from Github.
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 */
class Focus {
    private static final String BASE_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/project.mobile.focus.release.latest";
    private static final String CHAIN_OF_TRUST_URL = BASE_URL + "/artifacts/public/chain-of-trust.json";
    private static final String DOWNLOAD_URL = BASE_URL + "/artifacts/public/app-%s-%s-release-unsigned.apk";

    private String timestamp;
    private String downloadUrl;

    private Focus(String timestamp, String downloadUrl) {
        this.timestamp = timestamp;
        this.downloadUrl = downloadUrl;
    }

    static Focus findLatest(App app, DeviceEnvironment.ABI abi) {
        final String appName;
        switch (app) {
            case FIREFOX_FOCUS:
                appName = "focus";
                break;
            case FIREFOX_KLAR:
                appName = "klar";
                break;
            default:
                throw new RuntimeException("switch fallthrough");
        }
        final String abiAbbreviation;
        switch (abi) {
            case AARCH64:
                abiAbbreviation = "aarch64";
                break;
            case ARM:
                abiAbbreviation = "arm";
                break;
            default:
                throw new IllegalArgumentException("unsupported abi");
        }
        final String downloadUrl = String.format(DOWNLOAD_URL, appName, abiAbbreviation);
        final String timestamp = MozillaCIConsumer.findLatest(CHAIN_OF_TRUST_URL).getTimestamp();
        return new Focus(timestamp, downloadUrl);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
