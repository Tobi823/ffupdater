package de.marmaro.krt.ffupdater.version;

import com.google.common.base.Preconditions;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.version.rest.mozilla_ci.Response;
import de.marmaro.krt.ffupdater.version.rest.mozilla_ci.Sha256Hash;

/**
 * Source url for Firefox Release, Firefox Beta and Firefox Nightly
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.release.latest
 * https://www.apkmirror.com/apk/mozilla/firefox/
 *
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-beta/
 *
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
class Firefox {
    private static final String ARTIFACT_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.%s.latest.%s/artifacts/%s";
    private static final String CHAIN_OF_TRUST_ARTIFACT = "public/chain-of-trust.json";

    final private String timestamp;
    final private String downloadUrl;
    final private Sha256Hash hash;

    private Firefox(String timestamp, String downloadUrl, Sha256Hash hash) {
        Preconditions.checkNotNull(timestamp);
        Preconditions.checkNotNull(downloadUrl);
        Preconditions.checkNotNull(hash);
        this.timestamp = timestamp;
        this.downloadUrl = downloadUrl;
        this.hash = hash;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public Sha256Hash getHash() {
        return hash;
    }

    /**
     * Do the network request to get the latest version name and download url for Fenix.
     *
     * @return result or null
     */
    static Firefox findLatest(App app, DeviceEnvironment.ABI abi) {
        final String chainOfTrustUrl = String.format(ARTIFACT_URL, getNamespaceSuffix(app), getAbiAbbreviation(abi), CHAIN_OF_TRUST_ARTIFACT);
        final Response chainOfTrustResponse = ApiConsumer.consume(chainOfTrustUrl, Response.class);
        Preconditions.checkNotNull(chainOfTrustResponse);

        final String timestamp = chainOfTrustResponse.getTask().getCreated();
        final String downloadUrl = String.format(ARTIFACT_URL, getNamespaceSuffix(app), getAbiAbbreviation(abi), getArtifactNameForApk(abi));
        final Sha256Hash sha256Hash = chainOfTrustResponse.getArtifacts().get(getArtifactNameForApk(abi));
        return new Firefox(timestamp, downloadUrl, sha256Hash);
    }

    private static String getArtifactNameForApk(DeviceEnvironment.ABI abi) {
        return String.format("public/build/%s/target.apk", getAbiAbbreviation(abi));
    }

    private static String getAbiAbbreviation(DeviceEnvironment.ABI abi) {
        switch (abi) {
            case AARCH64:
                return "arm64-v8a";
            case ARM:
                return "armeabi-v7a";
            case X86:
                return "x86";
            case X86_64:
                return "x86_64";
            default:
                throw new RuntimeException("unsupported abi");
        }
    }

    private static String getNamespaceSuffix(App app) {
        switch (app) {
            case FIREFOX_RELEASE:
                return "release";
            case FIREFOX_BETA:
                return "beta";
            case FIREFOX_NIGHTLY:
                return "nightly";
            default:
                throw new RuntimeException("unsupported app - must be FIREFOX_RELEASE, FIREFOX_BETA or FIREFOX_NIGHTLY");
        }
    }
}
