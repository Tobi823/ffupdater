package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseVersion;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * https://github.com/mozilla-tw/FirefoxLite/releases
 * https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest
 */
class FirefoxLite implements Callable<AvailableMetadata> {
    public static final String OWNER = "mozilla-tw";
    public static final String REPOSITORY = "FirefoxLite";

    private final GithubConsumer githubConsumer;

    FirefoxLite(GithubConsumer githubConsumer) {
        Objects.requireNonNull(githubConsumer);
        this.githubConsumer = githubConsumer;
    }

    @Override
    public AvailableMetadata call() throws Exception {
        final GithubConsumer.GithubResult result = githubConsumer.consume(OWNER, REPOSITORY);
        final String tagName = result.getTagName().replace("v", "");
        final URL downloadUrl = result.getUrls().values().stream()
                .filter(url -> url.toString().endsWith(".apk"))
                .findFirst()
                .orElseThrow(() -> new ParamRuntimeException("no .apk file in assets"));
        return new AvailableMetadata(new ReleaseVersion(tagName), downloadUrl);
    }
}
