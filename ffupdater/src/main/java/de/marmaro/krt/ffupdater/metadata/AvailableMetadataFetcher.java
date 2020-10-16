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

public class AvailableMetadataFetcher {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<App, Future<AvailableMetadata>> futureCache = new HashMap<>();
    private final SharedPreferences sharedPreferences;
    private final DeviceEnvironment deviceEnvironment;

    public AvailableMetadataFetcher(SharedPreferences sharedPreferences, DeviceEnvironment deviceEnvironment) {
        Preconditions.checkNotNull(sharedPreferences);
        Preconditions.checkNotNull(deviceEnvironment);
        this.sharedPreferences = sharedPreferences;
        this.deviceEnvironment = deviceEnvironment;
    }

    public Map<App, Future<AvailableMetadata>> fetchMetadata(Set<App> apps) {
        return apps.stream().collect(Collectors.toMap(app -> app, app -> {
            Future<AvailableMetadata> cachedFuture = futureCache.get(app);
            if (cachedFuture != null && (cachedFuture.isCancelled() || cachedFuture.isDone())) {
                return cachedFuture;
            }

            Callable<AvailableMetadata> callable = new Fetcher(sharedPreferences, app, deviceEnvironment);
            Future<AvailableMetadata> future = executorService.submit(callable);

            futureCache.put(app, future);
            return future;
        }));
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
