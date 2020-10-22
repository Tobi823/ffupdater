package de.marmaro.krt.ffupdater.metadata.fetcher;

import android.content.SharedPreferences;
import android.net.TrafficStats;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

public class Fetcher implements Callable<AvailableMetadata> {
    public static final int FIREFOX_KLAR_TAG = 11;
    public static final int FIREFOX_FOCUS_TAG = 12;
    public static final int FIREFOX_LIGHT_TAG = 13;
    public static final int FIREFOX_RELEASE_TAG = 14;
    public static final int FIREFOX_BETA_TAG = 15;
    public static final int FIREFOX_NIGHTLY_TAG = 16;
    public static final int LOCKWISE_TAG = 17;
    public static final int BRAVE_TAG = 17;

    private final Cache cache;
    private final App app;
    private final DeviceEnvironment deviceEnvironment;
    private final MozillaCiConsumer mozillaCiConsumer;
    private final GithubConsumer githubConsumer;

    public Fetcher(SharedPreferences sharedPreferences,
                   App app,
                   DeviceEnvironment deviceEnvironment,
                   MozillaCiConsumer mozillaCiConsumer,
                   GithubConsumer githubConsumer) {
        Objects.requireNonNull(sharedPreferences);
        Objects.requireNonNull(app);
        Objects.requireNonNull(deviceEnvironment);
        Objects.requireNonNull(mozillaCiConsumer);
        Objects.requireNonNull(githubConsumer);
        this.cache = new Cache(sharedPreferences);
        this.app = app;
        this.deviceEnvironment = deviceEnvironment;
        this.mozillaCiConsumer = mozillaCiConsumer;
        this.githubConsumer = githubConsumer;
    }

    @Override
    public AvailableMetadata call() throws Exception {
        if (cache.isCacheUpToDate(app)) {
            Optional<AvailableMetadata> metadata = cache.getMetadata(app);
            if (metadata.isPresent()) {
                return metadata.get();
            }
        }
        final AvailableMetadata metadata = fetchAvailableMetadata(app);
        cache.updateCache(app, metadata);
        return metadata;
    }

    private AvailableMetadata fetchAvailableMetadata(App app) throws Exception {
        switch (app) {
            case FIREFOX_KLAR:
                TrafficStats.setThreadStatsTag(FIREFOX_KLAR_TAG);
                return new Focus(mozillaCiConsumer, app, deviceEnvironment).call();
            case FIREFOX_FOCUS:
                TrafficStats.setThreadStatsTag(FIREFOX_FOCUS_TAG);
                return new Focus(mozillaCiConsumer, app, deviceEnvironment).call();
            case FIREFOX_LITE:
                TrafficStats.setThreadStatsTag(FIREFOX_LIGHT_TAG);
                return new FirefoxLite(githubConsumer).call();
            case FIREFOX_RELEASE:
                TrafficStats.setThreadStatsTag(FIREFOX_RELEASE_TAG);
                return new Firefox(mozillaCiConsumer, app, deviceEnvironment).call();
            case FIREFOX_BETA:
                TrafficStats.setThreadStatsTag(FIREFOX_BETA_TAG);
                return new Firefox(mozillaCiConsumer, app, deviceEnvironment).call();
            case FIREFOX_NIGHTLY:
                TrafficStats.setThreadStatsTag(FIREFOX_NIGHTLY_TAG);
                return new Firefox(mozillaCiConsumer, app, deviceEnvironment).call();
            case LOCKWISE:
                TrafficStats.setThreadStatsTag(LOCKWISE_TAG);
                return new Lockwise(githubConsumer).call();
            case BRAVE:
                TrafficStats.setThreadStatsTag(BRAVE_TAG);
                return new Brave(githubConsumer, deviceEnvironment).call();
            default:
                throw new ParamRuntimeException("can't create callable for unknown app %s", app);
        }
    }
}
