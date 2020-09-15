package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.ApkMirrorHelper;
import de.marmaro.krt.ffupdater.App;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FirefoxLiteIT {

    static FirefoxLite firefoxLite;

    @BeforeClass
    public static void setUp() {
        firefoxLite = FirefoxLite.findLatest();
    }

    @Test
    public void verify_lite() throws IOException {
        assertNotNull(firefoxLite);
        final String version = firefoxLite.getVersion();
        final String downloadUrl = firefoxLite.getDownloadUrl();
        assertFalse(version.isEmpty());
        assertFalse(downloadUrl.isEmpty());

        // check if downloadUrl is valid
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
        urlConnection.setRequestMethod("GET"); //because HEAD does not work
        try {
            urlConnection.getInputStream();
        } finally {
            urlConnection.disconnect();
        }
        System.out.println(App.FIREFOX_LITE + " - downloadUrl: " + downloadUrl + " version: " + version);
    }

    @Test
    public void is_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final String feedUrl = "https://www.apkmirror.com/apk/mozilla/firefox-rocket-fast-and-lightweight-web-browser/feed/";
        final String latestApkMirrorTitle = ApkMirrorHelper.getRssFeedResponse(feedUrl).getTitle();
        final String expectedTitlePrefix = String.format("Firefox Lite — Fast and Lightweight Web Browser %s(", firefoxLite.getVersion());
        assertThat(latestApkMirrorTitle, startsWith(expectedTitlePrefix));
    }
}