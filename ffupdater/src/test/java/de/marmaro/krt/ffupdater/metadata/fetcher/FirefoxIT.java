package de.marmaro.krt.ffupdater.metadata.fetcher;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;
import de.marmaro.krt.ffupdater.metadata.fetcher.ApkMirrorHelper.RssFeedResponse;
import de.marmaro.krt.ffupdater.utils.CompareHelper;
import de.marmaro.krt.ffupdater.utils.Utils;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FirefoxIT {
    private final static Duration MAX_AGE_RELEASE = Duration.ofDays(28);
    private final static Duration MAX_AGE_BETA = Duration.ofDays(21);
    private final static Duration MAX_AGE_NIGHTLY = Duration.ofDays(7);

    private static final DeviceEnvironment arm64 = new DeviceEnvironment(Collections.singletonList(ABI.AARCH64), 30);
    private static final DeviceEnvironment arm32 = new DeviceEnvironment(Collections.singletonList(ABI.ARM), 30);
    private static final DeviceEnvironment x64 = new DeviceEnvironment(Collections.singletonList(ABI.X86_64), 30);
    private static final DeviceEnvironment x86 = new DeviceEnvironment(Collections.singletonList(ABI.X86), 30);
    private static MozillaCiConsumer mozillaCiConsumer;

    private static RssFeedResponse firefoxBetaRss;
    private static RssFeedResponse firefoxNightlyRss;

    @BeforeClass
    public static void staticSetUp() throws ParserConfigurationException, SAXException, IOException {
        firefoxBetaRss = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox-beta/feed/");
        firefoxNightlyRss = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox/feed/");

        mozillaCiConsumer = new MozillaCiConsumer(new ApiConsumer());
    }

    @Test
    public void is_release_arm64_availableAndUpToDate() throws Exception {
        isReleaseAvailableAndUpToDate(arm64, "2-android-apk-download");
    }

    @Test
    public void is_release_arm32_availableAndUpToDate() throws Exception {
        isReleaseAvailableAndUpToDate(arm32, "android-apk-download");
    }

    @Test
    public void is_release_x64_availableAndUpToDate() throws Exception {
        isReleaseAvailableAndUpToDate(x64, "4-android-apk-download");
    }

    @Test
    public void is_release_x32_availableAndUpToDate() throws Exception {
        isReleaseAvailableAndUpToDate(x86, "3-android-apk-download");
    }

    @Test
    public void is_beta_arm64_availableAndUpToDate() throws Exception {
        isBetaAvailableAndUpToDate(arm64, "2-android-apk-download");
    }

    @Test
    public void is_beta_arm32_availableAndUpToDate() throws Exception {
        isBetaAvailableAndUpToDate(arm32, "android-apk-download");
    }

    @Test
    public void is_beta_x64_availableAndUpToDate() throws Exception {
        isBetaAvailableAndUpToDate(x64, "4-android-apk-download");
    }

    @Test
    public void is_beta_x32_availableAndUpToDate() throws Exception {
        isBetaAvailableAndUpToDate(x86, "3-android-apk-download");
    }

    @Test
    public void is_nightly_arm64_availableAndUpToDate() throws Exception {
        isNightlyAvailableAndUpToDate(arm64);
    }

    @Test
    public void is_nightly_arm32_availableAndUpToDate() throws Exception {
        isNightlyAvailableAndUpToDate(arm32);
    }

    @Test
    public void is_nightly_x64_availableAndUpToDate() throws Exception {
        isNightlyAvailableAndUpToDate(x64);
    }

    @Test
    public void is_nightly_x32_availableAndUpToDate() throws Exception {
        isNightlyAvailableAndUpToDate(x86);
    }

    public static void isReleaseAvailableAndUpToDate(DeviceEnvironment deviceEnvironment, String internalNameApkMirror) throws Exception {
        final AvailableMetadataExtended metadata = new Firefox(mozillaCiConsumer, FIREFOX_RELEASE, deviceEnvironment).call();
        verifyDownloadLinkAvailable(metadata);
        verifyReleaseAgeIsNotTooOld(metadata, MAX_AGE_RELEASE);
        verifyHash(metadata, firefoxNightlyRss, Utils.createMap(
                "firefox", "firefox-browser-fast-private-safe-web-browser",
                "release", internalNameApkMirror));
    }

    private static void isBetaAvailableAndUpToDate(DeviceEnvironment deviceEnvironment, String internalNameApkMirror) throws Exception {
        final AvailableMetadataExtended metadata = new Firefox(mozillaCiConsumer, FIREFOX_BETA, deviceEnvironment).call();
        verifyDownloadLinkAvailable(metadata);
        verifyReleaseAgeIsNotTooOld(metadata, MAX_AGE_BETA);
        verifyHash(metadata, firefoxBetaRss, Utils.createMap(
                "firefox", "firefox-for-android",
                "release", internalNameApkMirror));
    }

    /**
     * Mozilla CI releases slightly different nightly APK files with different hashes => that's why I don't compare the hash values
     */
    private static void isNightlyAvailableAndUpToDate(DeviceEnvironment deviceEnvironment) throws Exception {
        final AvailableMetadataExtended metadata = new Firefox(mozillaCiConsumer, FIREFOX_NIGHTLY, deviceEnvironment).call();
        verifyDownloadLinkAvailable(metadata);
        verifyReleaseAgeIsNotTooOld(metadata, MAX_AGE_NIGHTLY);
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
                                   RssFeedResponse rssFeedResponse,
                                   Map<String, String> replacements) throws IOException {
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
}