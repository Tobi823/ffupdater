package de.marmaro.krt.ffupdater.metadata.fetcher;

import android.content.SharedPreferences;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.Hash;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FetcherTest {

    public static final String CHAIN_OF_TRUST_URL = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
            "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/chain-of-trust.json";
    public static final String ARTIFACT_NAME = "public/build/arm64-v8a/target.apk";
    public static final String HASH = "c0f213ebce8e6e5a383a4538b4e1cea2ffa1240f550cfdfda82218fb061f23e0";

    private final DeviceEnvironment arm64 = new DeviceEnvironment(Collections.singletonList(ABI.AARCH64), 30);
    private MozillaCiConsumer mozillaCiConsumer;
    private Fetcher fetcher;
    private ZonedDateTime now;

    @Before
    public void setUp() throws Exception {
        SharedPreferences sharedPreferences = new SPMockBuilder().createSharedPreferences();

        mozillaCiConsumer = mock(MozillaCiConsumer.class);
        now = ZonedDateTime.now();
        Hash hash = new Hash(Hash.Type.SHA256, HASH);
        when(mozillaCiConsumer.consume(new URL(CHAIN_OF_TRUST_URL), ARTIFACT_NAME)).thenReturn(
                new MozillaCiConsumer.MozillaCiResult(new ReleaseTimestamp(now), hash));

        fetcher = new Fetcher(sharedPreferences, App.FIREFOX_RELEASE, arm64, mozillaCiConsumer, mock(GithubConsumer.class));
    }

    @Test
    public void call_verifyThatSecondCallIsCached() throws Exception {
        {
            final AvailableMetadata actual = fetcher.call();
            assertTrue(now.isEqual(((ReleaseTimestamp) actual.getReleaseId()).getCreated()));

            final URL expectedUrl = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
            assertEquals(expectedUrl, actual.getDownloadUrl());
        }

        when(mozillaCiConsumer.consume(any(),any())).thenThrow(new RuntimeException("the second call of " +
                "MozillaCiConsumer::consume should never happend because the cache should cache the response."));

        {
            final AvailableMetadata actual = fetcher.call();
            assertTrue(now.isEqual(((ReleaseTimestamp) actual.getReleaseId()).getCreated()));

            final URL expectedUrl = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                    "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk");
            assertEquals(expectedUrl, actual.getDownloadUrl());
        }
    }
}