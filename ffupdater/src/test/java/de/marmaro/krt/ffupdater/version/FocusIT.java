package de.marmaro.krt.ffupdater.version;

import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.ApkMirrorHelper;
import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FocusIT {

    @Test
    public void verify_focus_arm() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_focus_aarch64() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.AARCH64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_focus_x86_shouldFail() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_focus_x8664_shouldFail() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void verify_klar_arm() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_klar_aarch64() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_klar_x86_shouldFail() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_klar_x8664_shouldFail() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void is_focus_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = focus.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDate actualReleaseDate = timestamp.toLocalDate();

        final LocalDate expectedReleaseDate = ApkMirrorHelper.getLatestPubDate("https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/feed/");
        assertEquals(expectedReleaseDate, actualReleaseDate);
    }

    @Test
    public void is_fenix_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = focus.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDate actualReleaseDate = timestamp.toLocalDate();

        final LocalDate expectedReleaseDate = ApkMirrorHelper.getLatestPubDate("https://www.apkmirror.com/apk/mozilla/firefox-klar-the-privacy-browser-2/feed/");
        assertEquals(expectedReleaseDate, actualReleaseDate);
    }

    private static void verify(App app, DeviceEnvironment.ABI abi) throws IOException {
        final Focus focus = Focus.findLatest(app, abi);
        final String downloadUrl = focus.getDownloadUrl();
        final String timestamp = focus.getTimestamp();
        assertThat(String.format("download url of %s with %s is empty", app, abi), downloadUrl, is(not(emptyString())));
        assertThat(String.format("timestamp of %s with %s is empty", app, abi), timestamp, is(not(emptyString())));

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final long daysOld = ChronoUnit.DAYS.between(now, parsedTimestamp);
        assertThat(String.format("timestamp of %s with %s is too old", app, abi), daysOld, lessThan(31L));

        // check if downloadUrl is valid
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
        urlConnection.setRequestMethod("HEAD");
        try {
            urlConnection.getInputStream();
        } finally {
            urlConnection.disconnect();
        }
        System.out.printf("%s (%s) - downloadUrl: %s timestamp: %s\n", app, abi, downloadUrl, timestamp);
    }
}