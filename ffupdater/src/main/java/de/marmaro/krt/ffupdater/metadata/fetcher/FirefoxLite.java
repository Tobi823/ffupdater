package de.marmaro.krt.ffupdater.metadata.fetcher;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseVersion;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * https://github.com/mozilla-tw/FirefoxLite/releases
 * https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest
 */
class FirefoxLite implements Callable<AvailableMetadata> {
    private final GithubConsumer githubConsumer;
    private final GithubConsumer.Request request;

    FirefoxLite(GithubConsumer githubConsumer) {
        Objects.requireNonNull(githubConsumer);
        this.githubConsumer = githubConsumer;
        request = new GithubConsumer.Request()
                .setOwnerOfRepository("mozilla-tw")
                .setRepositoryName("FirefoxLite")
                .setResultsPerPage(5)
                .setReleaseValidator(this::isReleaseValid);
    }

    private boolean isReleaseValid(GithubConsumer.Release release) {
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
        final GithubConsumer.GithubResult result = githubConsumer.consumeLatestReleaseFirst(request);
        final String tagName = result.getTagName().replace("v", "");
        final URL downloadUrl = result.getUrls().values().stream()
                .filter(url -> url.toString().endsWith(".apk"))
                .findFirst()
                .orElseThrow(() -> new ParamRuntimeException("no .apk file in assets"));
        return new AvailableMetadata(new ReleaseVersion(tagName), downloadUrl);
    }
}
