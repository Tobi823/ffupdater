package de.marmaro.krt.ffupdater.version;

import com.google.common.base.Preconditions;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

/**
 * Access the version name and the download url for Firefox Focus and Firefox Klar from Github.
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class Focus {
    private static final String ARTIFACT_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/project.mobile.focus.release.latest/artifacts/%s";
    private static final String CHAIN_OF_TRUST_ARTIFACT_NAME = "public/chain-of-trust.json";

    final private String timestamp;
    final private String downloadUrl;
    final private String hash;

    private Focus(String timestamp, String downloadUrl, String hash) {
        this.timestamp = timestamp;
        this.downloadUrl = downloadUrl;
        this.hash = hash;
    }

    static Focus findLatest(App app, DeviceEnvironment.ABI abi) {
//        final String chainOfTrustUrl = String.format(ARTIFACT_URL, CHAIN_OF_TRUST_ARTIFACT_NAME);
//        final Response chainOfTrustResponse = ApiConsumer.consume(chainOfTrustUrl, Response.class);
//        Preconditions.checkNotNull(chainOfTrustResponse);
//
//        final String timestamp = chainOfTrustResponse.getTask().getCreated();
//        final String downloadUrl = String.format(ARTIFACT_URL, getArtifactNameForApk(app, abi));
//        final Sha256Hash sha256Hash = chainOfTrustResponse.getArtifacts().get(getArtifactNameForApk(app, abi));
        return new Focus(null, null, null);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

//    public Sha256Hash getHash() {
//        return hash;
//    }

    private static String getArtifactNameForApk(App app, DeviceEnvironment.ABI abi) {
        return String.format("public/app-%s-%s-release-unsigned.apk", getAppName(app), getAbiAbbreviation(abi));
    }

    private static String getAbiAbbreviation(DeviceEnvironment.ABI abi) {
        switch (abi) {
            case AARCH64:
                return "aarch64";
            case ARM:
                return "arm";
            default:
                throw new IllegalArgumentException("unsupported abi");
        }
    }

    private static String getAppName(App app) {
        switch (app) {
            case FIREFOX_FOCUS:
                return "focus";
            case FIREFOX_KLAR:
                return "klar";
            default:
                throw new RuntimeException("unsupported app - must be FIREFOX_FOCUS or FIREFOX_KLAR");
        }
    }
}
