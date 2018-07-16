package de.marmaro.krt.ffupdater;


import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


/**
 * Created by Tobiwan on 05.02.2018.
 */
public class VersionCompareTest {

    String jsonResult;
    String nightlyVersion;
    String betaVersion;
    String releaseVersion;

    @Before
    public void setUp() {
        nightlyVersion = "63.0a1";
        betaVersion = "62.0b7";
        releaseVersion = "61.0";
        jsonResult = "{\n" +
                "  'nightly_version': '" + nightlyVersion + "',\n" +
                "  'beta_version': '" + betaVersion + "',\n" +
                "  'version': '" + releaseVersion + "'}";
    }

    @Test
    public void isUpdateAvailable_nothingInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestReleaseInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.RELEASE, new Version(releaseVersion + "", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void isUpdateAvailable_latestBetaInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.BETA, new Version(new String(betaVersion), 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(true, actual.isEmpty());
    }


    @Test
    public void isUpdateAvailable_latestNightlyInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.NIGHTLY, new Version(nightlyVersion, 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(true, actual.isEmpty());
    }


    @Test
    public void isUpdateAvailable_outdatedReleaseInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.RELEASE, new Version("58.1", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(Collections.singletonList(UpdateChannel.RELEASE), actual);
    }

    @Test
    public void isUpdateAvailable_outdatedBetaInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.BETA, new Version("62.0b6", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(Collections.singletonList(UpdateChannel.BETA), actual);
    }

    @Test
    public void isUpdateAvailable_outdatedNightlyInstalled_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.NIGHTLY, new Version("63", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertEquals(Collections.singletonList(UpdateChannel.NIGHTLY), actual);
    }
    @Test
    public void isUpdateAvailable_allOutdated_returnEmptyList() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

        LocalInstalledVersions version = new LocalInstalledVersions();
        version.setVersion(UpdateChannel.RELEASE, new Version("58.1", 0));
        version.setVersion(UpdateChannel.BETA, new Version("62.0b6", 0));
        version.setVersion(UpdateChannel.NIGHTLY, new Version("63", 0));

        List<UpdateChannel> actual = VersionCompare.isUpdateAvailable(mobileVersions, version);
        assertThat(actual, containsInAnyOrder(UpdateChannel.RELEASE, UpdateChannel.BETA, UpdateChannel.NIGHTLY));
    }
}