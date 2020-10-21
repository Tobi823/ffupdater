package de.marmaro.krt.ffupdater.metadata.fetcher;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseVersion;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * https://github.com/mozilla-tw/FirefoxLite/releases
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest
 */
class Brave implements Callable<AvailableMetadata> {
    private final GithubConsumer githubConsumer;
    private final GithubConsumer.Request request;
    private final DeviceEnvironment deviceEnvironment;

    Brave(GithubConsumer githubConsumer, DeviceEnvironment deviceEnvironment) {
        this.deviceEnvironment = deviceEnvironment;
        Objects.requireNonNull(githubConsumer);
        this.githubConsumer = githubConsumer;
        request = new GithubConsumer.Request()
                .setOwnerOfRepository("brave")
                .setRepositoryName("brave-browser")
                .setResultsPerPage(20)
                .setReleaseValidator(this::isReleaseValid);
    }

    private boolean isReleaseValid(GithubConsumer.Release release) {
        final String name = release.getName();
        if (name == null || !name.startsWith("Release ")) {
            return false;
        }
        if (release.isPrerelease()) {
            return false;
        }
        final List<GithubConsumer.Asset> assets = release.getAssets();
        if (assets == null) {
            return false;
        }
        for (GithubConsumer.Asset asset : assets) {
            if (asset.getName().endsWith(".apk")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AvailableMetadata call() {
        final GithubConsumer.GithubResult result = githubConsumer.consumeManyReleases(request);

        final URL downloadURL;
        final ABI abi = deviceEnvironment.getSupportedAbis().get(0);
        switch (abi) {
            case AARCH64:
                downloadURL = result.getUrls().get("BraveMonoarm64.apk");
                break;
            case ARM:
                downloadURL = result.getUrls().get("BraveMonoarm.apk");
                break;
            case X86:
                downloadURL = result.getUrls().get("BraveMonox86.apk");
                break;
            case X86_64:
                downloadURL = result.getUrls().get("BraveMonox64.apk");
                break;
            default:
                throw new ParamRuntimeException("unknown ABI %s - switch fallthrough", abi);
        }
        Objects.requireNonNull(downloadURL);

        final String versionName = result.getTagName().replace("v", "");
        return new AvailableMetadata(new ReleaseVersion(versionName), downloadURL);
    }
}
