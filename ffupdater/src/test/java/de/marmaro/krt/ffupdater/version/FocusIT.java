package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.ApkMirrorHelper;
import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static org.exparity.hamcrest.date.LocalDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FocusIT {

    static Document apkMirrorFirefoxFocus;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        apkMirrorFirefoxFocus = ApkMirrorHelper.getDocument("https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/feed/");
    }

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
    public void is_focus_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_focus_is_up_to_date("", DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void is_focus_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_focus_is_up_to_date("2-", DeviceEnvironment.ABI.ARM);
    }

    private static void check_focus_is_up_to_date(String apkMirrorId, DeviceEnvironment.ABI abi) throws ParserConfigurationException, SAXException, IOException {
        final Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, abi);

        // check if hashes matches
        // why? I want an error if the apps from Mozilla CI and APK Mirror are different
        {
            final String appVersionPageUrl = ApkMirrorHelper.getAppVersionPage(apkMirrorFirefoxFocus);
            final String[] urlParts = appVersionPageUrl.split("/");
            final String abiVersionPageSuffix = urlParts[urlParts.length - 1]
                    .replace("firefox-focus-private-browser", "firefox-focus-the-privacy-browser")
                    .replace("release", apkMirrorId + "android-apk-download");
            final String abiVersionPageUrl = appVersionPageUrl + abiVersionPageSuffix;
            final String hash = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(abiVersionPageUrl);

            if (!Objects.equals(hash, focus.getHash().getHash())) {
                final LocalDateTime apkMirrorTimestamp = ApkMirrorHelper.getLatestPubDate(apkMirrorFirefoxFocus);
                long hours = ChronoUnit.HOURS.between(apkMirrorTimestamp, LocalDateTime.now(ZoneOffset.UTC));

                if (hours < 0) {
                    fail("time difference between now and the release on APK mirror must never be negative");
                } else if (hours > 60) {
                    // wait 2.5 days because the APK Mirror community is not so fast
                    fail("the app from Mozilla-CI is different than the app from APK mirror - there must be a bug");
                } else {
                    System.err.printf("FIREFOX FOCUS (ignore this error because the latest release on APK Mirror is only %d hours old) hashes are different but skip - expected: %s, but was: %s\n",
                            hours,
                            hash,
                            focus.getHash().getHash());
                }
            }
            System.out.printf("FIREFOX_FOCUS (%s) SHA256-hash: %s\n", abi, hash);
            assertEquals(hash, focus.getHash().getHash());
        }

        // check that between Mozilla CI release and APK Mirror release are less than 72 hours
        // why? I want an error if Mozilla CI stops releasing new updates
        {
            final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(focus.getTimestamp()));
            final LocalDateTime expectedRelease = ApkMirrorHelper.getLatestPubDate(apkMirrorFirefoxFocus);
            assertThat(timestamp, within(72, ChronoUnit.HOURS, expectedRelease));
        }
    }

    @Test
    public void is_klar_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        final Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = focus.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDateTime expectedRelease = ApkMirrorHelper.getLatestPubDate(apkMirrorFirefoxFocus);

        // for releases which are only release on the Mozilla CI and not on APKMirror
        if (timestamp.isAfter(expectedRelease)) {
            // max 1 week difference
            assertThat(timestamp, within(7, ChronoUnit.DAYS, expectedRelease));
            System.out.println("Mozialla CI offers a non released version of FIREFOX_KLAR");
            return;
        }

        assertThat(timestamp, within(24, ChronoUnit.HOURS, expectedRelease));
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