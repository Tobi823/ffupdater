package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.net.URL;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirefoxLiteTest {

    private FirefoxLite firefoxLite;

    @Before
    public void setUp() throws Exception {
        final ApiConsumer apiConsumer = mock(ApiConsumer.class);
        final GithubConsumer githubConsumer = new GithubConsumer(apiConsumer);

        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest"),
                GithubConsumer.Release.class))
                .thenReturn(readResultFromJsonFile());

        firefoxLite = new FirefoxLite(githubConsumer);
    }

    private GithubConsumer.Release readResultFromJsonFile() {
        return new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("FirefoxLite_release_latest.json")), GithubConsumer.Release.class);
    }

    @Test
    public void call() throws Exception {
        final URL expected = new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk");

        final AvailableMetadata result = firefoxLite.call();
        assertEquals("2.5.1", result.getReleaseId().getValueAsString());
        assertEquals(expected, result.getDownloadUrl());
    }
}