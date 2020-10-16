package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.release.latest
 * https://www.apkmirror.com/apk/mozilla/firefox/
 *
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-beta/
 *
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
class Firefox implements Callable<AvailableMetadata> {
    private static final String BASE_URL =
            "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.%s.latest.%s/artifacts/%s";
    private static final String CHAIN_OF_TRUST_ARTIFACT = "public/chain-of-trust.json";
    public static final String APK_ARTIFACT = "public/build/%s/target.apk";

    private final DeviceEnvironment deviceEnvironment;
    private final String baseUrl;
    private final MozillaCiConsumer mozillaCiConsumer;
    private final App app;

    Firefox(MozillaCiConsumer mozillaCiConsumer, App app, DeviceEnvironment deviceEnvironment) {
        Preconditions.checkNotNull(mozillaCiConsumer);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(deviceEnvironment);
        this.deviceEnvironment = deviceEnvironment;
        this.mozillaCiConsumer = mozillaCiConsumer;
        this.app = app;
        this.baseUrl = String.format(BASE_URL, getNamespaceSuffix(), getAbiAbbreviation(), "%s");
    }

    @Override
    public AvailableMetadataExtended call() throws Exception {
        final MozillaCiConsumer.MozillaCiResult result;
        {
            final URL url = new URL(String.format(baseUrl, CHAIN_OF_TRUST_ARTIFACT));
            result = mozillaCiConsumer.consume(url, getArtifactNameForApk());
        }

        final URL downloadUrl = new URL(String.format(baseUrl, getArtifactNameForApk()));
        return new AvailableMetadataExtended(
                downloadUrl,
                result.getTimestamp(),
                result.getHash()
        );
    }

    private String getArtifactNameForApk() {
        return String.format(APK_ARTIFACT, getAbiAbbreviation());
    }

    private String getAbiAbbreviation() {
        switch (deviceEnvironment.getBestSuitedAbi()) {
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

    private String getNamespaceSuffix() {
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
