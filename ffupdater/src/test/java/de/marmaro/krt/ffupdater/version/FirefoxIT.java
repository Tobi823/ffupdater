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
public class FirefoxIT {

    static Document apkMirrorFirefoxBeta;
    static Document apkMirrorFirefoxRelease;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        apkMirrorFirefoxBeta = ApkMirrorHelper.getDocument("https://www.apkmirror.com/apk/mozilla/firefox-beta/feed/");
        apkMirrorFirefoxRelease = ApkMirrorHelper.getDocument("https://www.apkmirror.com/apk/mozilla/firefox/feed/");
    }

    @Test
    public void verify_release_aarch64() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void verify_release_arm() throws IOException {
        verify(App.FIREFOX_RELEASE, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_release_x64() throws IOException {
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
    public void verify_beta_x64() throws IOException {
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
    public void verify_nightly_x64() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void verify_nightly_x86() throws IOException {
        verify(App.FIREFOX_NIGHTLY, DeviceEnvironment.ABI.X86);
    }

    @Test
    public void is_release_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date(
                "firefox-browser-fast-private-safe-web-browser",
                "2-android-apk-download",
                App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64,
                apkMirrorFirefoxRelease);
    }

    @Test
    public void is_release_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date(
                "firefox-browser-fast-private-safe-web-browser",
                "android-apk-download",
                App.FIREFOX_RELEASE, DeviceEnvironment.ABI.ARM,
                apkMirrorFirefoxRelease);
    }

    @Test
    public void is_release_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date(
                "firefox-browser-fast-private-safe-web-browser",
                "3-android-apk-download",
                App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86,
                apkMirrorFirefoxRelease);
    }

    @Test
    public void is_release_x64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date(
                "firefox-browser-fast-private-safe-web-browser",
                "4-android-apk-download",
                App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86_64,
                apkMirrorFirefoxRelease);
    }

    @Test
    public void is_beta_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date("firefox-for-android",
                "2-android-apk-download",
                App.FIREFOX_BETA,
                DeviceEnvironment.ABI.AARCH64,
                apkMirrorFirefoxBeta);
    }

    @Test
    public void is_beta_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date("firefox-for-android",
                "android-apk-download",
                App.FIREFOX_BETA,
                DeviceEnvironment.ABI.ARM,
                apkMirrorFirefoxBeta);
    }

    @Test
    public void is_beta_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date(
                "firefox-for-android",
                "3-android-apk-download",
                App.FIREFOX_BETA,
                DeviceEnvironment.ABI.X86,
                apkMirrorFirefoxBeta);
    }

    @Test
    public void is_beta_x64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_if_hash_and_timestamp_are_up_to_date(
                "firefox-for-android",
                "4-android-apk-download",
                App.FIREFOX_BETA,
                DeviceEnvironment.ABI.X86_64,
                apkMirrorFirefoxBeta);
    }

    private static void check_if_hash_and_timestamp_are_up_to_date(String firefoxReplacement, String releaseReplacement, App app, DeviceEnvironment.ABI abi, Document document) throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(app, abi);

        // check if hashes matches
        // why? I want an error if the apps from Mozilla CI and APK Mirror are different
        {
            String appVersionPageUrl = ApkMirrorHelper.getAppVersionPage(document);
            String[] urlParts = appVersionPageUrl.split("/");
            String abiVersionPageSuffix = urlParts[urlParts.length - 1]
                    .replace("firefox", firefoxReplacement)
                    .replace("release", releaseReplacement);
            String abiVersionPageUrl = appVersionPageUrl + abiVersionPageSuffix;
            String hash = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(abiVersionPageUrl);

            if (!Objects.equals(hash, firefox.getHash().getHash())) {
                final LocalDateTime apkMirrorTimestamp = ApkMirrorHelper.getLatestPubDate(document);
                long hours = ChronoUnit.HOURS.between(apkMirrorTimestamp, LocalDateTime.now(ZoneOffset.UTC));

                if (hours < 0) {
                    fail("time difference between now and the release on APK mirror must never be negative");
                } else if (hours > 60) {
                    // wait 2.5 days because the APK Mirror community is not so fast
                    fail("the app from Mozilla-CI is different than the app from APK mirror - there must be a bug");
                } else {
                    System.err.printf("%s (ignore this error because the latest release on APK Mirror is only %d hours old) hashes are different but skip - expected: %s, but was: %s\n",
                            app,
                            hours,
                            hash,
                            firefox.getHash().getHash());
                }
            } else {
                System.out.printf("%s (%s) SHA256-hash: %s\n", app, abi, hash);
                assertEquals(hash, firefox.getHash().getHash());
            }
        }

        // check that between Mozilla CI release and APK Mirror release are less than 72 hours
        // why? I want an error if Mozilla CI stops releasing new updates
        {
            final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(firefox.getTimestamp()));
            final LocalDateTime expectedRelease = ApkMirrorHelper.getLatestPubDate(document);
            assertThat(timestamp, within(72, ChronoUnit.HOURS, expectedRelease));
        }
    }

    @Test
    public void is_nightly_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_nightly_is_up_to_date(DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void is_nightly_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_nightly_is_up_to_date(DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void is_nightly_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_nightly_is_up_to_date(DeviceEnvironment.ABI.X86);
    }

    @Test
    public void is_nightly_x64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_nightly_is_up_to_date(DeviceEnvironment.ABI.X86_64);
    }

    private static void check_nightly_is_up_to_date(DeviceEnvironment.ABI abi) throws ParserConfigurationException, SAXException, IOException {
        // check that between Mozilla CI release and APK Mirror release are less than 72 hours
        // why? I want an error if Mozilla CI stops releasing new updates
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_NIGHTLY, abi);
        final String timestampString = firefox.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDateTime expectedRelease = ApkMirrorHelper.getLatestPubDate(
                ApkMirrorHelper.getDocument("https://www.apkmirror.com/apk/mozilla/firefox-fenix/feed/"));

        assertThat(timestamp, within(48, ChronoUnit.HOURS, expectedRelease));
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