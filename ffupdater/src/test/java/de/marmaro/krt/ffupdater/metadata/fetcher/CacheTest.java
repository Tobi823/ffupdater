package de.marmaro.krt.ffupdater.metadata.fetcher;

import android.content.SharedPreferences;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;
import de.marmaro.krt.ffupdater.metadata.ReleaseVersion;

import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameInstant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheTest {

    private Cache cache;
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        sharedPreferences = new SPMockBuilder().createSharedPreferences();
        cache = new Cache(sharedPreferences);
    }

    @Test
    public void getMetadata_withReleaseVersion() throws MalformedURLException {
        final String expectedVersion = "2.1.4";
        final URL expectedUrl = new URL("https://github.com/mozilla-lockwise/lockwise-android/releases/download/" +
                "release-v4.0.0-RC-2/lockbox-app-release-6087-signed.apk");
        final AvailableMetadata metadata = new AvailableMetadata(new ReleaseVersion(expectedVersion), expectedUrl);

        cache.updateCache(App.FIREFOX_LITE, metadata);

        final AvailableMetadata actual = cache.getMetadata(App.FIREFOX_LITE)
                .orElseThrow(() -> new RuntimeException("missing metadata for app"));
        assertEquals(expectedVersion, actual.getReleaseId().getValueAsString());
        assertEquals(expectedUrl, actual.getDownloadUrl());
    }

    @Test
    public void getMetadata_withReleaseTimestamp() throws MalformedURLException {
        final ZonedDateTime expectedTimestamp = ZonedDateTime.now();
        final URL expectedUrl = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
        final AvailableMetadata metadata = new AvailableMetadata(new ReleaseTimestamp(expectedTimestamp), expectedUrl);

        cache.updateCache(App.FIREFOX_RELEASE, metadata);

        final AvailableMetadata actual = cache.getMetadata(App.FIREFOX_RELEASE)
                .orElseThrow(() -> new RuntimeException("missing metadata for app"));
        assertThat(((ReleaseTimestamp) actual.getReleaseId()).getCreated(), sameInstant(expectedTimestamp));
        assertEquals(expectedUrl, actual.getDownloadUrl());
    }

    @Test
    public void getMetadata_isStorageSeparateForEveryApp() throws MalformedURLException {
        final String app1Version = "2.1.4";
        final URL app1Url = new URL("https://github.com/mozilla-lockwise/lockwise-android/releases/download/" +
                "release-v4.0.0-RC-2/lockbox-app-release-6087-signed.apk");
        cache.updateCache(App.FIREFOX_LITE, new AvailableMetadata(new ReleaseVersion(app1Version), app1Url));

        final ZonedDateTime app2Timestamp = ZonedDateTime.now();
        final URL app2Url = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
        cache.updateCache(App.FIREFOX_RELEASE, new AvailableMetadata(new ReleaseTimestamp(app2Timestamp), app2Url));

        {
            final AvailableMetadata actual = cache.getMetadata(App.FIREFOX_LITE)
                    .orElseThrow(() -> new RuntimeException("missing metadata for app"));
            assertEquals(app1Version, actual.getReleaseId().getValueAsString());
            assertEquals(app1Url, actual.getDownloadUrl());
        }

        {
            final AvailableMetadata actual = cache.getMetadata(App.FIREFOX_RELEASE)
                    .orElseThrow(() -> new RuntimeException("missing metadata for app"));
            assertThat(((ReleaseTimestamp) actual.getReleaseId()).getCreated(), sameInstant(app2Timestamp));
            assertEquals(app2Url, actual.getDownloadUrl());
        }
    }

    @Test
    public void getMetadata_emptyStorage_returnEmptyOptional() {
        assertFalse(cache.getMetadata(App.FIREFOX_FOCUS).isPresent());
    }

    @Test
    public void isCacheUpToDate_withNewEntry_returnTrue() throws MalformedURLException {
        final URL expectedUrl = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
        final AvailableMetadata metadata = new AvailableMetadata(new ReleaseTimestamp(ZonedDateTime.now()), expectedUrl);

        cache.updateCache(App.FIREFOX_BETA, metadata);
        assertTrue(cache.isCacheUpToDate(App.FIREFOX_BETA));
    }

    @Test
    public void isCacheUpToDate_withNotTooOldEntry_returnTrue() throws MalformedURLException {
        final URL expectedUrl = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
        final AvailableMetadata metadata = new AvailableMetadata(new ReleaseTimestamp(ZonedDateTime.now()), expectedUrl);

        cache.updateCache(App.FIREFOX_BETA, metadata);

        long newValue = System.currentTimeMillis() - Duration.ofMinutes(9).toMillis();
        sharedPreferences.edit().putLong("download_metadata_" + App.FIREFOX_BETA + "_created_epoch_ms", newValue).commit();

        assertTrue(cache.isCacheUpToDate(App.FIREFOX_BETA));
    }

    @Test
    public void isCacheUpToDate_withOldEntry_returnFalse() throws MalformedURLException {
        final URL expectedUrl = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.nightly.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
        final AvailableMetadata metadata = new AvailableMetadata(new ReleaseTimestamp(ZonedDateTime.now()), expectedUrl);

        cache.updateCache(App.FIREFOX_BETA, metadata);

        long newValue = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis() - 1;
        sharedPreferences.edit().putLong("download_metadata_" + App.FIREFOX_BETA + "_created_epoch_ms", newValue).commit();

        assertFalse(cache.isCacheUpToDate(App.FIREFOX_BETA));
    }
}