package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
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

    static ApkMirrorHelper.RssFeedResponse firefoxBetaRssFeedResponse;
    static ApkMirrorHelper.RssFeedResponse firefoxReleaseRssFeedResponse;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        firefoxBetaRssFeedResponse = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox-beta/feed/");
        firefoxReleaseRssFeedResponse = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox/feed/");
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
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-browser-fast-private-safe-web-browser");
        replacements.put("release", "2-android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_RELEASE, DeviceEnvironment.ABI.AARCH64, firefoxReleaseRssFeedResponse);
    }

    @Test
    public void is_release_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-browser-fast-private-safe-web-browser");
        replacements.put("release", "android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_RELEASE, DeviceEnvironment.ABI.ARM, firefoxReleaseRssFeedResponse);
    }

    @Test
    public void is_release_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-browser-fast-private-safe-web-browser");
        replacements.put("release", "3-android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86, firefoxReleaseRssFeedResponse);
    }

    @Test
    public void is_release_x64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-browser-fast-private-safe-web-browser");
        replacements.put("release", "4-android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_RELEASE, DeviceEnvironment.ABI.X86_64, firefoxReleaseRssFeedResponse);
    }

    @Test
    public void is_beta_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-for-android");
        replacements.put("release", "2-android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_BETA, DeviceEnvironment.ABI.AARCH64, firefoxBetaRssFeedResponse);
    }

    @Test
    public void is_beta_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-for-android");
        replacements.put("release", "android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_BETA, DeviceEnvironment.ABI.ARM, firefoxBetaRssFeedResponse);
    }

    @Test
    public void is_beta_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-for-android");
        replacements.put("release", "3-android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_BETA, DeviceEnvironment.ABI.X86, firefoxBetaRssFeedResponse);
    }

    @Test
    public void is_beta_x64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox", "firefox-for-android");
        replacements.put("release", "4-android-apk-download");

        check_if_hash_and_timestamp_are_up_to_date(replacements, App.FIREFOX_BETA, DeviceEnvironment.ABI.X86_64, firefoxBetaRssFeedResponse);
    }

    private static void check_if_hash_and_timestamp_are_up_to_date(
            Map<String, String> replacements,
            App app,
            DeviceEnvironment.ABI abi,
            ApkMirrorHelper.RssFeedResponse rssFeedResponse) throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(app, abi);
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(firefox.getTimestamp()));
        final Duration ageOfRelease = Duration.between(timestamp, LocalDateTime.now(ZoneOffset.UTC));

        if (ageOfRelease.isNegative()) {
            fail("the age of the app release on Mozilla CI can never be negative");
        } else if (ageOfRelease.toHours() < 48) {
            // if the app is pretty new (app release was in the last 48 hours) then a different hash value is possible
            String hashFromApkMirror = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(rssFeedResponse, replacements);
            if (!Objects.equals(hashFromApkMirror, firefox.getHash().toString())) {
                String format = "%s (ignore this error because the latest release on Mozilla CI is only %d hours old and APK Mirror is not so fast) " +
                        "hashes are different but skip - expected: %s, but was: %s\n";
                System.err.printf(format, app, ageOfRelease.toHours(), hashFromApkMirror, firefox.getHash().toString());
            }
        } else if (ageOfRelease.toDays() < 21) {
            // the app is not new - the hashes must be equal
            String hashFromApkMirror = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(rssFeedResponse, replacements);
            assertEquals(hashFromApkMirror, firefox.getHash().toString());
        } else {
            fail("the app from Mozilla CI is too old");
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

        final String feedUrl = "https://www.apkmirror.com/apk/mozilla/firefox-fenix/feed/";
        final LocalDateTime expectedRelease = ApkMirrorHelper.getRssFeedResponse(feedUrl).getPubDate();

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