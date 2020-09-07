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
public class FirefoxIT {

    @Test
    public void verify_release_aarch64() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void verify_release_arm() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_release_x8664() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void verify_release_x86() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86);
    }

    @Test
    public void verify_beta_aarch64() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void verify_beta_arm() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_beta_x8664() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void verify_beta_x86() throws IOException {
        verify(App.FIREFOX_BETA, DeviceEnvironment.ABI.X86);
    }

    @Test
    public void verify_nightly_aarch64() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void verify_nightly_arm() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_nightly_x8664() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void verify_nightly_x86() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.X86);
    }

    @Test
    public void is_release_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = firefox.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDate actualReleaseDate = timestamp.toLocalDate();

        final LocalDate expectedReleaseDate = ApkMirrorHelper.getLatestPubDate("https://www.apkmirror.com/apk/mozilla/firefox/feed/");
        assertEquals(expectedReleaseDate, actualReleaseDate);
    }

    @Test
    public void is_beta_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_BETA, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = firefox.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDate actualReleaseDate = timestamp.toLocalDate();

        final LocalDate expectedReleaseDate = ApkMirrorHelper.getLatestPubDate("https://www.apkmirror.com/apk/mozilla/firefox-beta/feed/");
        assertEquals(expectedReleaseDate, actualReleaseDate);
    }

    @Test
    public void is_nightly_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = firefox.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDate actualReleaseDate = timestamp.toLocalDate();

        final LocalDate expectedReleaseDate = ApkMirrorHelper.getLatestPubDate("https://www.apkmirror.com/apk/mozilla/firefox-fenix/feed/");
        assertEquals(expectedReleaseDate, actualReleaseDate);
    }

    private static void verify(App app, DeviceEnvironment.ABI abi) throws IOException {
        final Firefox firefox = Firefox.findLatest(app, abi);
        final String downloadUrl = firefox.getDownloadUrl();
        final String timestamp = firefox.getTimestamp();
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