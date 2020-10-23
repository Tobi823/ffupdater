package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void setUp() {
        apiConsumer = Mockito.mock(ApiConsumer.class);
        githubConsumer = new GithubConsumer(apiConsumer);
    }

    @Test
    public void consumeLatestReleaseFirst_latestReleaseHasApkFile() throws MalformedURLException {
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest"), Release.class))
                .thenReturn(getReleaseFromJsonFile("GithubConsumerTest_releases_latest_with_apk.json"));

        final GithubConsumer.Request request = new GithubConsumer.Request()
                .setOwnerOfRepository("mozilla-tw")
                .setRepositoryName("FirefoxLite")
                .setResultsPerPage(5)
                .setReleaseValidator(release -> release.getAssets().stream().anyMatch(asset -> asset.getName().endsWith(".apk")));
        final GithubConsumer.GithubResult result = githubConsumer.consumeLatestReleaseFirst(request);
        assertEquals("v2.5.1", result.getTagName());

        final Map<String, URL> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("Github_v2.5.1_.20465.apk"));
        assertEquals(new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk"), urls.get("Github_v2.5.1_.20465.apk"));
    }

    @Test
    public void consumeLatestReleaseFirst_latestReleaseHasNoApkFile() throws MalformedURLException {
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest"), Release.class))
                .thenReturn(getReleaseFromJsonFile("GithubConsumerTest_releases_latest_without_apk.json"));
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases?per_page=5&page=1"), Release[].class))
                .thenReturn(getReleaseArrayFromJsonFile("GithubConsumerTest_releases_first_page_with_apk.json"));

        final GithubConsumer.Request request = new GithubConsumer.Request()
                .setOwnerOfRepository("mozilla-tw")
                .setRepositoryName("FirefoxLite")
                .setResultsPerPage(5)
                .setReleaseValidator(release -> release.getAssets().stream().anyMatch(asset -> asset.getName().endsWith(".apk")));
        final GithubConsumer.GithubResult result = githubConsumer.consumeLatestReleaseFirst(request);
        assertEquals("v2.5.1", result.getTagName());

        final Map<String, URL> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("Github_v2.5.1_.20465.apk"));
        assertEquals(new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk"), urls.get("Github_v2.5.1_.20465.apk"));
    }

    @Test
    public void consumeManyReleases_firstPageHasApk() throws MalformedURLException {
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases?per_page=5&page=1"), Release[].class))
                .thenReturn(getReleaseArrayFromJsonFile("GithubConsumerTest_releases_first_page_with_apk.json"));

        final GithubConsumer.Request request = new GithubConsumer.Request()
                .setOwnerOfRepository("mozilla-tw")
                .setRepositoryName("FirefoxLite")
                .setResultsPerPage(5)
                .setReleaseValidator(release -> release.getAssets().stream().anyMatch(asset -> asset.getName().endsWith(".apk")));
        final GithubConsumer.GithubResult result = githubConsumer.consumeManyReleases(request);
        assertEquals("v2.5.1", result.getTagName());

        final Map<String, URL> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("Github_v2.5.1_.20465.apk"));
        assertEquals(new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.5.1/Github_v2.5.1_.20465.apk"), urls.get("Github_v2.5.1_.20465.apk"));
    }

    @Test
    public void consumeManyReleases_firstPageHasNoApk() throws MalformedURLException {
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases?per_page=5&page=1"), Release[].class))
                .thenReturn(getReleaseArrayFromJsonFile("GithubConsumerTest_releases_first_page_without_apk.json"));
        when(apiConsumer.consume(new URL("https://api.github.com/repos/mozilla-tw/FirefoxLite/releases?per_page=5&page=2"), Release[].class))
                .thenReturn(getReleaseArrayFromJsonFile("GithubConsumerTest_releases_second_page_with_apk.json"));

        final GithubConsumer.Request request = new GithubConsumer.Request()
                .setOwnerOfRepository("mozilla-tw")
                .setRepositoryName("FirefoxLite")
                .setResultsPerPage(5)
                .setReleaseValidator(release -> release.getAssets().stream().anyMatch(asset -> asset.getName().endsWith(".apk")));
        final GithubConsumer.GithubResult result = githubConsumer.consumeManyReleases(request);
        assertEquals("v2.1.13", result.getTagName());

        final Map<String, URL> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("FirefoxLite_v2.1.13_19177.apk"));
        assertEquals(new URL("https://github.com/mozilla-tw/FirefoxLite/releases/download/v2.1.13/FirefoxLite_v2.1.13_19177.apk"), urls.get("FirefoxLite_v2.1.13_19177.apk"));
    }

    private Release getReleaseFromJsonFile(String s) {
        return new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream(s)), Release.class);
    }

    private Release[] getReleaseArrayFromJsonFile(String s) {
        return new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream(s)), Release[].class);
    }

}