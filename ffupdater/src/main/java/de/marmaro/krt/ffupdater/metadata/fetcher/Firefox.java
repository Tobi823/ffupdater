package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.ExtendedMetadata;
import de.marmaro.krt.ffupdater.metadata.Metadata;

class Firefox implements Callable<Metadata> {
    private static final String ARTIFACT_URL =
            "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/mobile.v2.fenix.%s.latest.%s/artifacts/%s";
    private static final String CHAIN_OF_TRUST_ARTIFACT_NAME = "public/chain-of-trust.json";

    private final DeviceEnvironment.ABI abi;
    private final String baseUrl;
    private final MozillaCiConsumer mozillaCiConsumer;

    Firefox(MozillaCiConsumer mozillaCiConsumer, App app, DeviceEnvironment.ABI abi) {
        Preconditions.checkNotNull(mozillaCiConsumer);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(abi);
        this.abi = abi;
        this.baseUrl = String.format(ARTIFACT_URL, getNamespaceSuffix(app), getAbiAbbreviation(abi), "%s");
        this.mozillaCiConsumer = mozillaCiConsumer;
    }

    @Override
    public ExtendedMetadata call() throws Exception {
        final MozillaCiConsumer.MozillaCiResult result;
        {
            final URL url = new URL(String.format(baseUrl, CHAIN_OF_TRUST_ARTIFACT_NAME));
            result = mozillaCiConsumer.consume(url, getArtifactNameForApk(abi));
        }

        final URL downloadUrl = new URL(String.format(baseUrl, getArtifactNameForApk(abi)));
        return new ExtendedMetadata(
                downloadUrl,
                result.getTimestamp(),
                result.getHash()
        );
    }

    private String getArtifactNameForApk(DeviceEnvironment.ABI abi) {
        return String.format("public/build/%s/target.apk", getAbiAbbreviation(abi));
    }

    private String getAbiAbbreviation(DeviceEnvironment.ABI abi) {
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

    private String getNamespaceSuffix(App app) {
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
