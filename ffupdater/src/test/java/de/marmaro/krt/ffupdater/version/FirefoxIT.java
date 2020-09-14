package de.marmaro.krt.ffupdater.version;

import org.junit.Test;
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
    public void is_release_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_release_is_up_to_date("2-", DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void is_release_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_release_is_up_to_date("", DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void is_release_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_release_is_up_to_date("3-", DeviceEnvironment.ABI.X86);
    }

    @Test
    public void is_release_x64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_release_is_up_to_date("4-", DeviceEnvironment.ABI.X86_64);
    }

    private static void check_release_is_up_to_date(String apkMirrorId, DeviceEnvironment.ABI abi) throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_RELEASE, abi);
        final String feedUrl = "https://www.apkmirror.com/apk/mozilla/firefox/feed/";
        String appVersionPageUrl = ApkMirrorHelper.getAppVersionPage(feedUrl);
        String[] urlParts = appVersionPageUrl.split("/");
        String abiVersionPageSuffix = urlParts[urlParts.length - 1]
                .replace("firefox", "firefox-browser-fast-private-safe-web-browser")
                .replace("release", apkMirrorId + "android-apk-download");
        String abiVersionPageUrl = appVersionPageUrl + abiVersionPageSuffix;
        String hash = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(abiVersionPageUrl);

        if (!Objects.equals(hash, firefox.getHash().getHash())) {
            System.err.println("hashes are different - expected: " + hash +  ", but was: " + firefox.getHash().getHash());
            final LocalDateTime apkMirrorTimestamp = ApkMirrorHelper.getLatestPubDate(feedUrl);
            long hours = ChronoUnit.HOURS.between(apkMirrorTimestamp, LocalDateTime.now(ZoneOffset.UTC));

            if (hours < 0) {
                fail("time difference between now and the release on APK mirror must never be negative");
            }
            if (hours < 48) {
                fail("the released app on APK mirror seems to be up-to-date but have a different hash - do I use the wrong mozilla-ci download url?");
            }
            // between 2 - 7 days: APK mirror is not sometime slow and does not release the update immediately
            if (hours > 7 * 24) {
                fail("the app on APK mirror was not updated for 7 days and its a different app then on the mozilla-ci server. There must be a bug.");
            }
            return;
        }
        System.out.printf("FIREFOX_RELEASE (%s) SHA256-hash: %s\n", abi, hash);
        assertEquals(hash, firefox.getHash().getHash());
    }

    @Test
    public void is_beta_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_beta_is_up_to_date("2-", DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void is_beta_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_beta_is_up_to_date("", DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void is_beta_x86_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_beta_is_up_to_date("3-", DeviceEnvironment.ABI.X86);
    }

    @Test
    public void is_beta_X64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        check_beta_is_up_to_date("4-", DeviceEnvironment.ABI.X86_64);
    }

    private static void check_beta_is_up_to_date(String apkMirrorId, DeviceEnvironment.ABI abi) throws ParserConfigurationException, SAXException, IOException {
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_BETA, abi);
        final String feedUrl = "https://www.apkmirror.com/apk/mozilla/firefox-beta/feed/";
        String appVersionPageUrl = ApkMirrorHelper.getAppVersionPage(feedUrl);
        String[] urlParts = appVersionPageUrl.split("/");
        String abiVersionPageSuffix = urlParts[urlParts.length - 1]
                .replace("firefox", "firefox-for-android")
                .replace("release", apkMirrorId + "android-apk-download");
        String abiVersionPageUrl = appVersionPageUrl + abiVersionPageSuffix;
        String hash = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(abiVersionPageUrl);

        if (!Objects.equals(hash, firefox.getHash().getHash())) {
            System.err.println("hashes are different - expected: " + hash +  ", but was: " + firefox.getHash().getHash());
            final LocalDateTime apkMirrorTimestamp = ApkMirrorHelper.getLatestPubDate(feedUrl);
            long hours = ChronoUnit.HOURS.between(apkMirrorTimestamp, LocalDateTime.now(ZoneOffset.UTC));

            if (hours < 0) {
                fail("time difference between now and the release on APK mirror must never be negative");
            }
            if (hours < 48) {
                fail("the released app on APK mirror seems to be up-to-date but have a different hash - do I use the wrong mozilla-ci download url?");
            }
            // between 2 - 7 days: APK mirror is not sometime slow and does not release the update immediately
            if (hours > 7 * 24) {
                fail("the app on APK mirror was not updated for 7 days and its a different app then on the mozilla-ci server. There must be a bug.");
            }
            return;
        }
        System.out.printf("FIREFOX_NIGHTLY (%s) SHA256-hash: %s\n", abi, hash);
        assertEquals(hash, firefox.getHash().getHash());
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
        final Firefox firefox = Firefox.findLatest(App.FIREFOX_NIGHTLY, abi);
        final String timestampString = firefox.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDateTime expectedRelease = ApkMirrorHelper.getLatestPubDate("https://www.apkmirror.com/apk/mozilla/firefox-fenix/feed/");

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