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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class LockwiseIT {

    static Lockwise lockwise;

    @BeforeClass
    public static void setUp() {
        lockwise = Lockwise.findLatest();
    }

    @Test
    public void verify_lockwise() throws IOException {
        assertNotNull(lockwise);
        final String version = lockwise.getVersion();
        final String downloadUrl = lockwise.getDownloadUrl();
        assertFalse(version.isEmpty());
        assertFalse(downloadUrl.isEmpty());

        // check if downloadUrl is valid
        System.out.println(downloadUrl);
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
        urlConnection.setRequestMethod("GET"); //because HEAD does not work
        try {
            urlConnection.getInputStream();
        } finally {
            urlConnection.disconnect();
        }
        System.out.println(App.LOCKWISE + " - downloadUrl: " + downloadUrl + " version: " + version);
    }

    @Test
    public void is_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final String latestApkMirrorTitle = ApkMirrorHelper.getLatestTitle("https://www.apkmirror.com/apk/mozilla/firefox-lockwise/feed/");
        final String expectedTitlePrefix = String.format("Firefox Lockwise %s by Mozilla", lockwise.getVersion());
        assertEquals(latestApkMirrorTitle, expectedTitlePrefix);
    }
}