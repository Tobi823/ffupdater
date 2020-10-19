package de.marmaro.krt.ffupdater.metadata;

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;

import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateCheckerTest {

    private UpdateChecker updateChecker;

    @Before
    public void setUp() throws Exception {
        updateChecker = new UpdateChecker();
    }

    @Test
    public void isUpdateAvailable_timestamp_sameTimestamp_returnFalse() throws MalformedURLException {
        final ZonedDateTime now = ZonedDateTime.now();
        AvailableMetadata availableMetadata = new AvailableMetadata(
                new ReleaseTimestamp(now),
                new URL("https://api"));
        InstalledMetadata installedMetadata = new InstalledMetadata(
                "1.0.0",
                new ReleaseTimestamp(now));
        assertFalse(updateChecker.isUpdateAvailable(FIREFOX_RELEASE, installedMetadata, availableMetadata));
    }

    @Test
    public void isUpdateAvailable_timestamp_differentTimestamp_returnTrue() throws MalformedURLException {
        final ZonedDateTime now = ZonedDateTime.now();
        AvailableMetadata availableMetadata = new AvailableMetadata(
                new ReleaseTimestamp(now),
                new URL("https://api"));
        InstalledMetadata installedMetadata = new InstalledMetadata(
                "1.0.0",
                new ReleaseTimestamp(now.minusDays(3)));
        assertTrue(updateChecker.isUpdateAvailable(FIREFOX_RELEASE, installedMetadata, availableMetadata));
    }

    @Test
    public void isUpdateAvailable_version_sameVersion_returnFalse() throws MalformedURLException {
        final ZonedDateTime now = ZonedDateTime.now();
        AvailableMetadata availableMetadata = new AvailableMetadata(
                new ReleaseVersion("1.0.0"),
                new URL("https://api"));
        InstalledMetadata installedMetadata = new InstalledMetadata(
                "1.0.0",
                new ReleaseVersion("1.0.0"));
        assertFalse(updateChecker.isUpdateAvailable(FIREFOX_LITE, installedMetadata, availableMetadata));
    }

    @Test
    public void isUpdateAvailable_version_differentVersion_returnTrue() throws MalformedURLException {
        final ZonedDateTime now = ZonedDateTime.now();
        AvailableMetadata availableMetadata = new AvailableMetadata(
                new ReleaseVersion("1.0.0"),
                new URL("https://api"));
        InstalledMetadata installedMetadata = new InstalledMetadata(
                "1.0.1",
                new ReleaseVersion("1.0.1"));
        assertTrue(updateChecker.isUpdateAvailable(FIREFOX_LITE, installedMetadata, availableMetadata));
    }
}