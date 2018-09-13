package de.marmaro.krt.ffupdater;


import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.marmaro.krt.ffupdater.api.ApiResponses;
import de.marmaro.krt.ffupdater.api.github.Release;
import de.marmaro.krt.ffupdater.api.mozilla.MobileVersions;
import de.marmaro.krt.ffupdater.version.VersionCompare;
import de.marmaro.krt.ffupdater.version.VersionExtractor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;


/**
 * Created by Tobiwan on 05.02.2018.
 */
public class VersionCompareTest {

    Map<UpdateChannel, Version> available;

    String nightlyVersion = "63.0a1";
    String betaVersion = "62.0";
    String releaseVersion = "61.0";
    String focusVersion = "6.0";
    String klarVersion = "6.0";

    @Before
    public void setUp() throws IOException {
        InputStream mobileVersionsStream = getClass().getClassLoader().getResourceAsStream("mobile_versions.json");
        String mobileVersionsResult = IOUtils.toString(mobileVersionsStream, StandardCharsets.UTF_8.name());

        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(mobileVersionsResult, MobileVersions.class);

        InputStream releasesStream = getClass().getClassLoader().getResourceAsStream("releases.json");
        String releasesResult = IOUtils.toString(releasesStream, StandardCharsets.UTF_8.name());

        GsonBuilder gsonBuilder2 = new GsonBuilder();
        Release release = gsonBuilder2.create().fromJson(releasesResult, Release.class);

        ApiResponses responses = new ApiResponses(mobileVersions, release);
        available = new VersionExtractor(responses).getVersionStrings();
    }

    @Test
    public void isUpdateAvailable_nothingInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestReleaseInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.RELEASE, new Version(releaseVersion, 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestBetaInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.BETA, new Version(betaVersion, 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestNightlyInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.NIGHTLY, new Version(nightlyVersion, 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestFocusInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.FOCUS, new Version(focusVersion, 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestKlarInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.KLAR, new Version(klarVersion, 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_outdatedReleaseInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.RELEASE, new Version("58.1", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(Collections.singletonList(UpdateChannel.RELEASE), actual);
    }

    @Test
    public void isUpdateAvailable_outdatedBetaInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.BETA, new Version("62.0b6", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(Collections.singletonList(UpdateChannel.BETA), actual);
    }

    @Test
    public void isUpdateAvailable_outdatedNightlyInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.NIGHTLY, new Version("63", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(Collections.singletonList(UpdateChannel.NIGHTLY), actual);
    }
    @Test
    public void isUpdateAvailable_outdatedFocusInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.FOCUS, new Version("5.2", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(Collections.singletonList(UpdateChannel.FOCUS), actual);
    }
    @Test
    public void isUpdateAvailable_outdatedKlarInstalled_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.KLAR, new Version("5.1", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertEquals(Collections.singletonList(UpdateChannel.KLAR), actual);
    }

    @Test
    public void isUpdateAvailable_allOutdated_returnEmptyList() {
        Map<UpdateChannel, Version> installed = new HashMap<>();
        installed.put(UpdateChannel.RELEASE, new Version("58.1", 0));
        installed.put(UpdateChannel.BETA, new Version("62.0b6", 0));
        installed.put(UpdateChannel.NIGHTLY, new Version("63", 0));
        installed.put(UpdateChannel.FOCUS, new Version("5.2", 0));
        installed.put(UpdateChannel.KLAR, new Version("5.1", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(available, installed);
        assertThat(actual, containsInAnyOrder(UpdateChannel.RELEASE, UpdateChannel.BETA,
                UpdateChannel.NIGHTLY, UpdateChannel.FOCUS, UpdateChannel.KLAR));
    }
}