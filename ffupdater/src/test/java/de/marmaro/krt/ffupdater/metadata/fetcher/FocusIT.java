package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;
import de.marmaro.krt.ffupdater.utils.CompareHelper;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;
import de.marmaro.krt.ffupdater.utils.Utils;

import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static org.exparity.hamcrest.date.LocalDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FocusIT {
    private final static Duration MAX_AGE = Duration.ofDays(28);
    private static DeviceEnvironment arm64;
    private static DeviceEnvironment arm32;
    private static DeviceEnvironment x64;
    private static DeviceEnvironment x86;
    private static MozillaCiConsumer mozillaCiConsumer;

    private static ApkMirrorHelper.RssFeedResponse focusRss;
    private static ApkMirrorHelper.RssFeedResponse klarRss;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        focusRss = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/feed/");
        klarRss = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox-klar-the-privacy-browser-2/feed/");

        arm64 = mock(DeviceEnvironment.class);
        when(arm64.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.AARCH64);

        arm32 = mock(DeviceEnvironment.class);
        when(arm32.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.ARM);

        x64 = mock(DeviceEnvironment.class);
        when(x64.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.X86_64);

        x86 = mock(DeviceEnvironment.class);
        when(x86.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.X86);

        mozillaCiConsumer = new MozillaCiConsumer(new ApiConsumer());
    }

    @Test
    public void is_focus_arm64_availableAndUpToDate() throws Exception {
        isFocusAvailableAndUpToDate(arm64, "android-apk-download");
    }

    @Test
    public void is_focus_arm32_availableAndUpToDate() throws Exception {
        isFocusAvailableAndUpToDate(arm32, "2-android-apk-download");
    }

    @Test(expected = ParamRuntimeException.class)
    public void is_focus_x64_availableAndUpToDate() throws Exception {
        isFocusAvailableAndUpToDate(x64, "");
    }

    @Test(expected = ParamRuntimeException.class)
    public void is_focus_x32_availableAndUpToDate() throws Exception {
        isFocusAvailableAndUpToDate(x86, "");
    }

    @Test
    public void is_klar_arm64_availableAndUpToDate() throws Exception {
        isKlarAvailableAndUpToDate(arm64, "2-android-apk-download");
    }

    @Test
    public void is_klar_arm32_availableAndUpToDate() throws Exception {
        isKlarAvailableAndUpToDate(arm32, "android-apk-download");
    }

    @Test(expected = ParamRuntimeException.class)
    public void is_klar_x64_availableAndUpToDate() throws Exception {
        isKlarAvailableAndUpToDate(x64, "");
    }

    @Test(expected = ParamRuntimeException.class)
    public void is_klar_x32_availableAndUpToDate() throws Exception {
        isKlarAvailableAndUpToDate(x86, "");
    }

    private static void isFocusAvailableAndUpToDate(DeviceEnvironment deviceEnvironment, String internalNameApkMirror) throws Exception {
        final AvailableMetadataExtended metadata = new Focus(mozillaCiConsumer, FIREFOX_FOCUS, deviceEnvironment).call();
        verifyDownloadLinkAvailable(metadata);
        verifyReleaseAgeIsNotTooOld(metadata, MAX_AGE);
        verifyHash(metadata, focusRss, Utils.createMap(
                "firefox-focus-private-browser", "firefox-focus-the-privacy-browser",
                "release", internalNameApkMirror));
    }

    private static void isKlarAvailableAndUpToDate(DeviceEnvironment deviceEnvironment, String internalNameApkMirror) throws Exception {
        final AvailableMetadataExtended metadata = new Focus(mozillaCiConsumer, FIREFOX_KLAR, deviceEnvironment).call();
        verifyDownloadLinkAvailable(metadata);
        verifyReleaseAgeIsNotTooOld(metadata, MAX_AGE);
        //TODO
//        verifyHash(metadata, klarRss, Utils.createMap(
//                "firefox-klar-the-privacy-browser-2", "firefox-klar-the-privacy-browser",
//                "release", internalNameApkMirror));
    }

    private static void verifyDownloadLinkAvailable(AvailableMetadata availableMetadata) throws IOException {
        final HttpsURLConnection connection = (HttpsURLConnection) availableMetadata.getDownloadUrl().openConnection();
        assertThat(connection.getResponseCode(), allOf(greaterThanOrEqualTo(200), lessThan(300)));
        connection.disconnect();
    }

    private static void verifyReleaseAgeIsNotTooOld(AvailableMetadata metadata, Duration maxAge) {
        final ReleaseTimestamp timestamp = (ReleaseTimestamp) metadata.getReleaseId();
        final Duration age = Duration.between(timestamp.getCreated(), ZonedDateTime.now());

        assertFalse("age must never be zero", age.isNegative());
        assertFalse("due to network lag its impossible that the app was released in the current instant", age.isZero());
        assertTrue("release must not be older than 31 days", new CompareHelper<>(age).isLessThan(maxAge));
    }

    private static void verifyHash(AvailableMetadataExtended metadata,
                                   ApkMirrorHelper.RssFeedResponse rssFeedResponse,
                                   Map<String, String> replacements) throws ParserConfigurationException, SAXException, IOException {
        final ReleaseTimestamp timestamp = (ReleaseTimestamp) metadata.getReleaseId();
        final Duration age = Duration.between(timestamp.getCreated(), ZonedDateTime.now());

        final String hash = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(rssFeedResponse, replacements);
        // it can take up to two days until the latest release is available on APKMirror
        // => if the release is older than two days, then the hashes must be equal
        if (!Objects.equals(hash, metadata.getHash().getHexString())) {
            if (new CompareHelper<>(age).isGreaterOrEqualTo(Duration.ofDays(2))) {
                assertEquals("hashes are not equal", hash, metadata.getHash().getHexString());
            }
        }
    }


//
//    @Test
//    public void verify_focus_aarch64() throws IOException {
//        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.AARCH64);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void verify_focus_x86_shouldFail() throws IOException {
//        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void verify_focus_x8664_shouldFail() throws IOException {
//        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86_64);
//    }
//
//    @Test
//    public void verify_klar_arm() throws IOException {
//        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.ARM);
//    }
//
//    @Test
//    public void verify_klar_aarch64() throws IOException {
//        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void verify_klar_x86_shouldFail() throws IOException {
//        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void verify_klar_x8664_shouldFail() throws IOException {
//        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86_64);
//    }
//

//
//    @Test
//    public void is_klar_up_to_date() {
//        final Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
//        final String timestampString = focus.getTimestamp();
//        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
//        final LocalDateTime expectedRelease = rssFeedResponse.getPubDate();
//
//        final Duration ageOfRelease = Duration.between(timestamp, LocalDateTime.now(ZoneOffset.UTC));
//        if (ageOfRelease.isNegative()) {
//            fail("the age of the app release on Mozilla CI can never be negative");
//        } else if (ageOfRelease.toHours() < 48) {
//            // if the app is pretty new (app release was in the last 48 hours) then a different hash value is possible
//            String format = "%s (ignore this error because the latest release on Mozilla CI is only %d hours old and APK Mirror is not so fast) " +
//                    "there is a time difference between Mozilla CI release and APK Mirror release of %d days\n";
//            final Duration timeDiff = Duration.between(expectedRelease, timestamp);
//            if (timeDiff.toHours() > 48) {
//                System.err.printf(format, App.FIREFOX_KLAR, ageOfRelease.toHours(), timeDiff.toDays());
//            }
//        } else if (ageOfRelease.toDays() < 21) {
//            assertTrue(timestamp.isBefore(expectedRelease)); //because APKMirror is slow => expectedRelease should always after the faster release on Mozilla CI
//            assertThat(timestamp, within(4, ChronoUnit.DAYS, expectedRelease));
//        } else {
//            fail("the app from Mozilla CI is too old");
//        }
//    }
//
//    private static void verify(App app, DeviceEnvironment.ABI abi) throws IOException {
//        final Focus focus = Focus.findLatest(app, abi);
//        final String downloadUrl = focus.getDownloadUrl();
//        final String timestamp = focus.getTimestamp();
//        assertThat(String.format("download url of %s with %s is empty", app, abi), downloadUrl, is(not(emptyString())));
//        assertThat(String.format("timestamp of %s with %s is empty", app, abi), timestamp, is(not(emptyString())));
//
//        final LocalDateTime now = LocalDateTime.now();
//        final LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//        final long daysOld = ChronoUnit.DAYS.between(now, parsedTimestamp);
//        assertThat(String.format("timestamp of %s with %s is too old", app, abi), daysOld, lessThan(31L));
//
//        // check if downloadUrl is valid
//        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
//        urlConnection.setRequestMethod("HEAD");
//        try {
//            urlConnection.getInputStream();
//        } finally {
//            urlConnection.disconnect();
//        }
//        System.out.printf("%s (%s) - downloadUrl: %s timestamp: %s\n", app, abi, downloadUrl, timestamp);
//    }
//
//    private static class IgnoreHash {
//        private String mozillaCiHash;
//        private String apkMirrorHash;
//
//        public IgnoreHash(String mozillaCiHash, String apkMirrorHash) {
//            this.mozillaCiHash = mozillaCiHash;
//            this.apkMirrorHash = apkMirrorHash;
//        }
//
//        public String getMozillaCiHash() {
//            return mozillaCiHash;
//        }
//
//        public String getApkMirrorHash() {
//            return apkMirrorHash;
//        }
//    }
}