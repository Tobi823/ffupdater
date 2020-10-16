package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class Focus implements Callable<AvailableMetadata> {
    public static final String BASE_URL =
            "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/project.mobile.focus.release.latest/artifacts/%s";
    public static final String CHAIN_OF_TRUST_ARTIFACT = "public/chain-of-trust.json";
    public static final String APK_ARTIFACT = "public/app-%s-%s-release-unsigned.apk";

    private final App app;
    private final DeviceEnvironment deviceEnvironment;
    private final MozillaCiConsumer mozillaCiConsumer;

    Focus(MozillaCiConsumer mozillaCiConsumer, App app, DeviceEnvironment deviceEnvironment) {
        Preconditions.checkNotNull(mozillaCiConsumer);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(deviceEnvironment);
        this.mozillaCiConsumer = mozillaCiConsumer;
        this.app = app;
        this.deviceEnvironment = deviceEnvironment;
    }

    @Override
    public AvailableMetadataExtended call() throws Exception {
        final MozillaCiConsumer.MozillaCiResult result;
        {
            final URL url = new URL(String.format(BASE_URL, CHAIN_OF_TRUST_ARTIFACT));
            result = mozillaCiConsumer.consume(url, getArtifactNameForApk());
        }

        final URL downloadUrl = new URL(String.format(BASE_URL, getArtifactNameForApk()));
        return new AvailableMetadataExtended(
                downloadUrl,
                result.getTimestamp(),
                result.getHash()
        );
    }

    private String getArtifactNameForApk() {
        return String.format(APK_ARTIFACT, getAppName(), getAbiAbbreviation());
    }

    private String getAbiAbbreviation() {
        switch (deviceEnvironment.getBestSuitedAbi()) {
            case AARCH64:
                return "aarch64";
            case ARM:
                return "arm";
            default:
                throw new ParamRuntimeException("unsupported abi %s", deviceEnvironment.getBestSuitedAbi());
        }
    }

    private String getAppName() {
        switch (app) {
            case FIREFOX_FOCUS:
                return "focus";
            case FIREFOX_KLAR:
                return "klar";
            default:
                throw new ParamRuntimeException("unsupported app %s - must be FIREFOX_FOCUS or FIREFOX_KLAR", app);
        }
    }
}
