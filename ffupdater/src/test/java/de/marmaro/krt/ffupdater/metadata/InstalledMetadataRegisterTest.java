package de.marmaro.krt.ffupdater.metadata;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;

import de.marmaro.krt.ffupdater.App;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static de.marmaro.krt.ffupdater.App.LOCKWISE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstalledMetadataRegisterTest {

    private PackageManager packageManager;
    private InstalledMetadataRegister register;

    @Before
    public void setUp() {
        packageManager = mock(PackageManager.class);
        register = new InstalledMetadataRegister(packageManager, new SPMockBuilder().createSharedPreferences());
    }

    @Test
    public void getInstalledApps_allAppsInstalled_returnAllApps() throws PackageManager.NameNotFoundException {
        for (App app : App.values()) {
            when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo());
        }
        assertThat(register.getInstalledApps(), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_allAppsInstalled_returnEmptySet() throws PackageManager.NameNotFoundException {
        for (App app : App.values()) {
            when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo());
        }
        assertTrue(register.getNotInstalledApps().isEmpty());
    }

    @Test
    public void getInstalledApps_noAppsInstalled_returnEmptySet() throws PackageManager.NameNotFoundException {
        for (App app : App.values()) {
            when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());
        }
        assertTrue(register.getInstalledApps().isEmpty());
    }

    @Test
    public void getNotInstalledApps_noAppsInstalled_returnAllApps() throws PackageManager.NameNotFoundException {
        for (App app : App.values()) {
            when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());
        }
        assertThat(register.getNotInstalledApps(), containsInAnyOrder(App.values()));
    }

    @Test
    public void getMetadata_appNotInstalled_returnEmptyOptional() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_RELEASE.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(register.getMetadata(FIREFOX_RELEASE).isPresent());
    }

    @Test
    public void getMetadata_firefoxRelease() throws PackageManager.NameNotFoundException {
        checkReleaseTimestampBased(FIREFOX_RELEASE);
    }

    @Test
    public void getMetadata_firefoxBeta() throws PackageManager.NameNotFoundException {
        checkReleaseTimestampBased(FIREFOX_BETA);
    }

    @Test
    public void getMetadata_firefoxNightly() throws PackageManager.NameNotFoundException {
        checkReleaseTimestampBased(FIREFOX_NIGHTLY);
    }

    @Test
    public void getMetadata_firefoxFocus() throws PackageManager.NameNotFoundException {
        checkReleaseTimestampBased(FIREFOX_FOCUS);
    }

    @Test
    public void getMetadata_firefoxKlar() throws PackageManager.NameNotFoundException {
        checkReleaseTimestampBased(FIREFOX_KLAR);
    }

    @Test
    public void getMetadata_firefoxLite() throws PackageManager.NameNotFoundException {
        checkReleaseVersionBased(FIREFOX_LITE);
    }

    @Test
    public void getMetadata_lockwise() throws PackageManager.NameNotFoundException {
        checkReleaseVersionBased(LOCKWISE);
    }

    private void checkReleaseTimestampBased(App app) throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo());

        final ZonedDateTime now = ZonedDateTime.now();
        register.saveReleaseId(app, new ReleaseTimestamp(now));

        final InstalledMetadata metadata = register.getMetadata(app).
                orElseThrow(() -> new RuntimeException("missing installed metadata"));

        assertEquals("1.0.0", metadata.getVersionName());
        assertTrue(now.isEqual(((ReleaseTimestamp)metadata.getInstalledReleasedId()).getCreated()));
    }

    private void checkReleaseVersionBased(App app) throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo());
        register.saveReleaseId(app, new ReleaseVersion("1.0.0"));

        final InstalledMetadata metadata = register.getMetadata(app).
                orElseThrow(() -> new RuntimeException("missing installed metadata"));

        assertEquals("1.0.0", metadata.getVersionName());
        assertEquals("1.0.0", metadata.getInstalledReleasedId().getValueAsString());
    }

    private static PackageInfo createPackageInfo() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = "1.0.0";
        return packageInfo;
    }
}