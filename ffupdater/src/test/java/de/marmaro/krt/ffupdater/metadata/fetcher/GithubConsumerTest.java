package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import de.marmaro.krt.ffupdater.metadata.fetcher.GithubConsumer.Release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GithubConsumerTest {

    private ApiConsumer apiConsumer;
    private GithubConsumer githubConsumer;

    @Before
    public void setUp() throws Exception {
        apiConsumer = Mockito.mock(ApiConsumer.class);
        githubConsumer = new GithubConsumer(apiConsumer);
    }

    @Test
    public void consume_latestReleaseHasApkFile() throws MalformedURLException, FileNotFoundException {
        final InputStreamReader releaseWithApk = new InputStreamReader(getClass().getResourceAsStream("GithubConsumerTest_release_with_apk.json"));
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest"), Release.class))
                .thenReturn(new Gson().fromJson(releaseWithApk, Release.class));

        final GithubConsumer.GithubResult result = githubConsumer.consume("mozilla-tw", "FirefoxLite");
        assertEquals("v2.5.1", result.getTagName());

        final Map<String, URL> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("Github_v2.5.1_.20465.apk"));
        assertEquals(new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk"), urls.get("Github_v2.5.1_.20465.apk"));
    }

    @Test
    public void consume_latestReleaseHasNoApkFile() throws MalformedURLException {
        final InputStreamReader releaseWithNoApk = new InputStreamReader(getClass().getResourceAsStream("GithubConsumerTest_release_with_no_apk.json"));
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest"), Release.class))
                .thenReturn(new Gson().fromJson(releaseWithNoApk, Release.class));

        final InputStreamReader allReleases = new InputStreamReader(getClass().getResourceAsStream("GithubConsumerTest_all_releases.json"));
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases"), Release[].class))
                .thenReturn(new Gson().fromJson(allReleases, Release[].class));

        final GithubConsumer.GithubResult result = githubConsumer.consume("mozilla-tw", "FirefoxLite");
        assertEquals("v2.5.1", result.getTagName());

        final Map<String, URL> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("Github_v2.5.1_.20465.apk"));
        assertEquals(new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk"), urls.get("Github_v2.5.1_.20465.apk"));
    }

}