package de.marmaro.krt.ffupdater.version;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.marmaro.krt.ffupdater.App;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FirefoxLiteIT {

    @Test
    public void verify_lite() throws IOException {
        final FirefoxLite firefoxLite = FirefoxLite.findLatest();
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
}