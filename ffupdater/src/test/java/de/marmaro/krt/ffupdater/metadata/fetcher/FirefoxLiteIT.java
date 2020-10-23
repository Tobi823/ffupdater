package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.StringStartsWith.startsWith;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FirefoxLiteIT {
    private AvailableMetadata metadata;

    @Before
    public void setUp() {
        final GithubConsumer githubConsumer = new GithubConsumer(new ApiConsumer());
        metadata = new FirefoxLite(githubConsumer).call();
    }

    @Test
    public void isDownloadUrlAvailable() throws IOException {
        final HttpsURLConnection connection = (HttpsURLConnection) metadata.getDownloadUrl().openConnection();
        assertThat(connection.getResponseCode(), allOf(greaterThanOrEqualTo(200), lessThan(300)));
        connection.disconnect();
    }

    @Test
    public void isAppUpToDate() throws ParserConfigurationException, SAXException, IOException {
        final String url = "https://www.apkmirror.com/apk/mozilla/firefox-rocket-fast-and-lightweight-web-browser/feed/";
        final String expectedTitle = ApkMirrorHelper.getRssFeedResponse(url).getTitle();
        final String actualTitle = String.format("Firefox Lite — Fast and Lightweight Web Browser %s(", metadata.getReleaseId().getValueAsString());
        assertThat(expectedTitle, startsWith(actualTitle));
    }
}