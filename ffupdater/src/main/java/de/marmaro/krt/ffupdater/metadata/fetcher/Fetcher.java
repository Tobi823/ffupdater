package de.marmaro.krt.ffupdater.metadata.fetcher;

import android.content.SharedPreferences;
import android.net.TrafficStats;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.ParamRuntimeException;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

public class Fetcher implements Callable<AvailableMetadata> {
    public static final int FIREFOX_KLAR_TAG = 11;
    public static final int FIRFOX_FOCUS_TAG = 12;
    public static final int FIREFOX_LIGHT_TAG = 13;
    public static final int FIREFOX_RELEASE_TAG = 14;
    public static final int FIREFOX_BETA_TAG = 15;
    public static final int FIREFOX_NIGHTLY_TAG = 16;
    public static final int LOCKWISE_TAG = 17;
    private final Cache cache;
    private final App app;
    private final DeviceEnvironment deviceEnvironment;

    public Fetcher(SharedPreferences sharedPreferences, App app, DeviceEnvironment deviceEnvironment) {
        Preconditions.checkNotNull(sharedPreferences);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(deviceEnvironment);
        this.cache = new Cache(sharedPreferences);
        this.app = app;
        this.deviceEnvironment = deviceEnvironment;
    }

    @Override
    public AvailableMetadata call() throws Exception {
        if (cache.isCacheUpToDate(app)) {
            Optional<AvailableMetadata> metadata = cache.getMetadata(app);
            if (metadata.isPresent()) {
                return metadata.get();
            }
        }

        final AvailableMetadata metadata;
        switch (app) {
            case FIREFOX_KLAR:
                TrafficStats.setThreadStatsTag(FIREFOX_KLAR_TAG);
                metadata = new Focus(app, deviceEnvironment).call();
                break;
            case FIREFOX_FOCUS:
                TrafficStats.setThreadStatsTag(FIRFOX_FOCUS_TAG);
                metadata = new Focus(app, deviceEnvironment).call();
                break;
            case FIREFOX_LITE:
                TrafficStats.setThreadStatsTag(FIREFOX_LIGHT_TAG);
                metadata = new FirefoxLite(deviceEnvironment).call();
                break;
            case FIREFOX_RELEASE:
                TrafficStats.setThreadStatsTag(FIREFOX_RELEASE_TAG);
                metadata = new Firefox(new MozillaCiConsumer(new ApiConsumer()), app, deviceEnvironment).call();
                break;
            case FIREFOX_BETA:
                TrafficStats.setThreadStatsTag(FIREFOX_BETA_TAG);
                metadata = new Firefox(new MozillaCiConsumer(new ApiConsumer()), app, deviceEnvironment).call();
                break;
            case FIREFOX_NIGHTLY:
                TrafficStats.setThreadStatsTag(FIREFOX_NIGHTLY_TAG);
                metadata = new Firefox(new MozillaCiConsumer(new ApiConsumer()), app, deviceEnvironment).call();
                break;
            case LOCKWISE:
                TrafficStats.setThreadStatsTag(LOCKWISE_TAG);
                metadata = new Lockwise(deviceEnvironment).call();
                break;
            default:
                throw new ParamRuntimeException("can't create callable for unknown app %s", app);
        }

        cache.updateCache(app, metadata);
        return metadata;
    }
}
