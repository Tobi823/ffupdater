package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStreamReader;
import java.net.URL;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class LockwiseTest {

    private Lockwise lockwise;

    @Before
    public void setUp() throws Exception {
        final ApiConsumer apiConsumer = Mockito.mock(ApiConsumer.class);
        final GithubConsumer githubConsumer = new GithubConsumer(apiConsumer);

        when(apiConsumer.consume(
                new URL("https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest"),
                GithubConsumer.Release.class))
                .thenReturn(readResultFromJsonFile());

        lockwise = new Lockwise(githubConsumer);
    }

    private GithubConsumer.Release readResultFromJsonFile() {
        return new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("Lockwise_release_latest.json")), GithubConsumer.Release.class);
    }

    @Test
    public void call() throws Exception {
        final URL expectedUrl = new URL("https://github.com/mozilla-lockwise/lockwise-android/releases/download/" +
                "release-v4.0.0-RC-2/lockbox-app-release-6087-signed.apk");

        final AvailableMetadata result = lockwise.call();
        assertEquals("4.0.0", result.getReleaseId().getValueAsString());
        assertEquals(expectedUrl, result.getDownloadUrl());
    }
}