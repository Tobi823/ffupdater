package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseVersion;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * https://github.com/brave/brave-browser
 * https://www.apkmirror.com/apk/brave-software/brave-browser
 */
class Iceraven implements Callable<AvailableMetadata> {
    private final GithubConsumer githubConsumer;
    private final GithubConsumer.Request request;
    private final DeviceEnvironment deviceEnvironment;

    Iceraven(GithubConsumer githubConsumer, DeviceEnvironment deviceEnvironment) {
        this.deviceEnvironment = Objects.requireNonNull(deviceEnvironment);
        this.githubConsumer = Objects.requireNonNull(githubConsumer);
        request = new GithubConsumer.Request()
                .setOwnerOfRepository("fork-maintainers")
                .setRepositoryName("iceraven-browser")
                .setResultsPerPage(5)
                .setReleaseValidator(this::isReleaseValid);
    }

    private boolean isReleaseValid(GithubConsumer.Release release) {
        if (release.isPrerelease()) {
            return false;
        }
        return release.getAssets().stream()
                .map(GithubConsumer.Asset::getName)
                .anyMatch(name -> name.endsWith(".apk"));
    }

    @Override
    public AvailableMetadata call() {
        final GithubConsumer.GithubResult result = githubConsumer.consumeManyReleases(request);
        final String versionName = result.getTagName().replace("iceraven-", "");

        final Map<String, URL> assets = result.getUrls();
        final ABI abi = deviceEnvironment.getSupportedAbis().get(0);
        final String assetName = findAssetNameForAbi(abi, assets.keySet());
        final URL assetUrl = assets.get(assetName);

        return new AvailableMetadata(new ReleaseVersion(versionName), assetUrl);
    }

    private String findAssetNameForAbi(ABI abi, Set<String> assetNames) {
        final String suffix = getAssetSuffixWithAbi(abi);
        return assetNames.stream()
                .filter(name -> name.endsWith(suffix))
                .reduce((a, b) -> {
                    // NoSuchElementException in case the stream is empty
                    throw new IllegalStateException("multiple asset names found: " + a + ", " + b);
                })
                .orElseThrow(IllegalStateException::new);
    }

    private String getAssetSuffixWithAbi(ABI abi) {
        switch (abi) {
            case AARCH64:
                return "browser-arm64-v8a-forkRelease.apk";
            case ARM:
                return "browser-armeabi-v7a-forkRelease.apk";
            case X86:
                return "browser-x86-forkRelease.apk";
            case X86_64:
                return "browser-x86_64-forkRelease.apk";
            default:
                throw new ParamRuntimeException("unknown ABI %s - switch fallthrough", abi);
        }
    }
}
