package de.marmaro.krt.ffupdater.metadata.fetcher;

import android.content.SharedPreferences;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.ParamRuntimeException;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.Metadata;

public class Fetcher implements Callable<Metadata> {
    private final Cache cache;
    private final App app;
    private final DeviceEnvironment.ABI abi;

    public Fetcher(Cache cache, App app, DeviceEnvironment.ABI abi) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(abi);
        this.cache = cache;
        this.app = app;
        this.abi = abi;
    }

    @Override
    public Metadata call() throws Exception {
        if (cache.isCacheUpToDate(app)) {
            Optional<Metadata> metadata = cache.getMetadata(app);
            if (metadata.isPresent()) {
                return metadata.get();
            }
        }

        final Metadata metadata;
        switch (app) {
            case FIREFOX_KLAR:
            case FIREFOX_FOCUS:
                metadata = new Focus(app, abi).call();
                break;
            case FIREFOX_LITE:
                metadata = new FirefoxLite(abi).call();
                break;
            case FIREFOX_RELEASE:
            case FIREFOX_BETA:
            case FIREFOX_NIGHTLY:
                metadata = new Firefox(new MozillaCiConsumer(new ApiConsumer()), app, abi).call();
                break;
            case LOCKWISE:
                metadata = new Lockwise(abi).call();
                break;
            default:
                throw new ParamRuntimeException("can't create callable for unknown app %s", app);
        }

        cache.updateCache(app, metadata);
        return metadata;
    }
}
