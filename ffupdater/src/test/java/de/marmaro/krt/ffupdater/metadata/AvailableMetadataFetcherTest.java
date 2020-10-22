package de.marmaro.krt.ffupdater.metadata;

import android.content.SharedPreferences;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class AvailableMetadataFetcherTest {

    private AvailableMetadataFetcher fetcher;

    @Before
    public void setUp() {
        final DeviceEnvironment arm64 = new DeviceEnvironment(Collections.singletonList(ABI.AARCH64), 30);
        final SharedPreferences sharedPreferences = new SPMockBuilder().createSharedPreferences();
        fetcher = new AvailableMetadataFetcher(sharedPreferences, arm64);
    }

    @Test
    public void fetchMetadata_useCache() throws InterruptedException, ExecutionException, TimeoutException {
        final App app = FIREFOX_RELEASE;
        final Map<App, Future<AvailableMetadata>> futures1 = fetcher.fetchMetadata(Collections.singletonList(app));
        final Map<App, Future<AvailableMetadata>> futures2 = fetcher.fetchMetadata(Collections.singletonList(app));

        // both futures must be the same
        assertSame(futures1.get(app), futures2.get(app));

        futures1.get(app).get(30, TimeUnit.SECONDS);

        // the second call is faster because it's cashed
        futures2.get(app).get(1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void fetchMetadata_useNotTheCache() throws InterruptedException, ExecutionException, TimeoutException {
        final App app = FIREFOX_BETA;
        final Map<App, Future<AvailableMetadata>> futures1 = fetcher.fetchMetadata(Collections.singletonList(app));
        futures1.get(app).get(30, TimeUnit.SECONDS);

        final Map<App, Future<AvailableMetadata>> futures2 = fetcher.fetchMetadata(Collections.singletonList(app));

        // both futures must be the same
        assertNotSame(futures1.get(app), futures2.get(app));
    }
}