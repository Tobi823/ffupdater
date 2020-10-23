package de.marmaro.krt.ffupdater.metadata.fetcher;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 * https://www.apkmirror.com/apk/mozilla/firefox-klar-the-privacy-browser-2/
 */
class Focus implements Callable<AvailableMetadata> {
    public static final String BASE_URL =
            "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/project.mobile.focus.release.latest/artifacts/%s";
    public static final String CHAIN_OF_TRUST_ARTIFACT = "public/chain-of-trust.json";
    public static final String APK_ARTIFACT = "public/app-%s-%s-release-unsigned.apk";

    private final MozillaCiConsumer mozillaCiConsumer;
    private final String abiAbbreviation;
    private final String appName;

    Focus(MozillaCiConsumer mozillaCiConsumer, App app, DeviceEnvironment deviceEnvironment) {
        Objects.requireNonNull(mozillaCiConsumer);
        Objects.requireNonNull(app);
        Objects.requireNonNull(deviceEnvironment);
        this.mozillaCiConsumer = mozillaCiConsumer;
        abiAbbreviation = getAbiAbbreviation(deviceEnvironment);
        appName = getAppName(app);
    }

    @Override
    public AvailableMetadataExtended call() throws Exception {
        final String apkArtifactName = String.format(APK_ARTIFACT, appName, abiAbbreviation);
        final URL chainOfTrustArtifact = new URL(String.format(BASE_URL, CHAIN_OF_TRUST_ARTIFACT));

        final MozillaCiConsumer.MozillaCiResult result = mozillaCiConsumer.consume(chainOfTrustArtifact, apkArtifactName);
        return new AvailableMetadataExtended(
                new URL(String.format(BASE_URL, apkArtifactName)),
                result.getTimestamp(),
                result.getHash()
        );
    }

    private String getAbiAbbreviation(DeviceEnvironment deviceEnvironment) {
        for (ABI abi : deviceEnvironment.getSupportedAbis()) {
            switch (abi) {
                case AARCH64:
                    return "aarch64";
                case ARM:
                    return "arm";
            }
        }
        throw new ParamRuntimeException("unsupported abi %s", deviceEnvironment.getSupportedAbis());
    }

    private String getAppName(App app) {
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
