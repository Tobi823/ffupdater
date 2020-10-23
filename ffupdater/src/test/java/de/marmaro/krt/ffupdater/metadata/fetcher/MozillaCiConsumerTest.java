package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import de.marmaro.krt.ffupdater.metadata.fetcher.MozillaCiConsumer.MozillaCiResult;
import de.marmaro.krt.ffupdater.metadata.fetcher.MozillaCiConsumer.Response;

import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameInstant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MozillaCiConsumerTest {

    private MozillaCiResult result;

    @Before
    public void setUp() throws MalformedURLException {
        final URL url = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/chain-of-trust.json");
        final InputStreamReader chainOfTrustJson = new InputStreamReader(getClass().getResourceAsStream("MozillaCiConsumerTest.json"));
        final ApiConsumer apiConsumer = mock(ApiConsumer.class);
        when(apiConsumer.consume(url, Response.class)).thenReturn(
                new Gson().fromJson(chainOfTrustJson, Response.class));

        final MozillaCiConsumer mozillaCiConsumer = new MozillaCiConsumer(apiConsumer);
        result = mozillaCiConsumer.consume(url, "public/build/arm64-v8a/target.apk");
        assertNotNull(result);
    }

    @Test
    public void isTimestampCorrect() {
        ZonedDateTime expectedTimestamp = ZonedDateTime.of(2020, 10, 16, 1, 25, 23, 565000000, ZoneId.of("UTC"));
        assertThat(result.getTimestamp().getCreated(), sameInstant(expectedTimestamp));
    }

    @Test
    public void isHashCorrect() {
        assertEquals("f56063913211d44de579b8335fe1146bd65aa0a35628d48852cb50171e9fa8fc", result.getHash().getHexString());
    }
}