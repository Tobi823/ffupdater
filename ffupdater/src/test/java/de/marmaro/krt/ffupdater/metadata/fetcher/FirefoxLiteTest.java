package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.utils.Utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FirefoxLiteTest {

    private FirefoxLite firefoxLite;

    private URL downloadUrl;

    @Before
    public void setUp() throws Exception {
        downloadUrl = new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk");

        final GithubConsumer githubConsumer = Mockito.mock(GithubConsumer.class);
        when(githubConsumer.consume("mozilla-tw", "FirefoxLite")).thenReturn(
             new GithubConsumer.GithubResult("v2.5.1", Utils.createMap("Github_v2.5.1_.20465.apk", downloadUrl))
        );

        firefoxLite = new FirefoxLite(githubConsumer);
    }

    @Test
    public void call() throws Exception {
        final AvailableMetadata result = firefoxLite.call();
        assertEquals("2.5.1", result.getReleaseId().getValueAsString());
        assertEquals(downloadUrl, result.getDownloadUrl());
    }
}