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
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest
 */
class Lockwise implements Callable<AvailableMetadata> {
    public static final String OWNER = "mozilla-lockwise";
    public static final String REPOSITORY = "lockwise-android";

    private final GithubConsumer githubConsumer;

    Lockwise(GithubConsumer githubConsumer) {
        Objects.requireNonNull(githubConsumer);
        this.githubConsumer = githubConsumer;
    }

    @Override
    public AvailableMetadata call() throws Exception {
        final GithubConsumer.GithubResult result = githubConsumer.consume(OWNER, REPOSITORY);
        final String versionName = result.getTagName().split("v")[1].split("-")[0];
        final URL downloadUrl = result.getUrls().values().stream()
                .filter(url -> url.toString().endsWith(".apk"))
                .findFirst()
                .orElseThrow(() -> new ParamRuntimeException("no .apk file in assets"));
        return new AvailableMetadata(new ReleaseVersion(versionName), downloadUrl);
    }
}
