package de.marmaro.krt.ffupdater.metadata;

import android.content.SharedPreferences;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.fetcher.Fetcher;

public class MetadataFetcher {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<App, Future<Metadata>> futureCache = new HashMap<>();
    private final SharedPreferences sharedPreferences;
    private final DeviceEnvironment.ABI abi;

    public MetadataFetcher(SharedPreferences sharedPreferences, DeviceEnvironment.ABI abi) {
        Preconditions.checkNotNull(sharedPreferences);
        Preconditions.checkNotNull(abi);
        this.sharedPreferences = sharedPreferences;
        this.abi = abi;
    }

    public Map<App, Future<Metadata>> fetchMetadata(Set<App> apps) {
        return apps.stream().collect(Collectors.toMap(app -> app, app -> {
            Future<Metadata> cachedFuture = futureCache.get(app);
            if (cachedFuture != null && (cachedFuture.isCancelled() || cachedFuture.isDone())) {
                return cachedFuture;
            }

            Callable<Metadata> callable = new Fetcher(sharedPreferences, app, abi);
            Future<Metadata> future = executorService.submit(callable);

            futureCache.put(app, future);
            return future;
        }));
    }

    public void shutdown() {
        //TODO implement
    }
}
