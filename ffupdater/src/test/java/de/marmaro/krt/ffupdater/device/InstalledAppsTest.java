package de.marmaro.krt.ffupdater.device;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstalledAppsTest {
    private static final String FENNEC_RELEASE_PACKAGE_NAME = "org.mozilla.firefox";
    private static final String FIREFOX_KLAR_PACKAGE_NAME = "org.mozilla.klar";
    private static final String FIREFOX_FOCUS_PACKAGE_NAME = "org.mozilla.focus";
    private static final String FIREFOX_LITE_PACKAGE_NAME = "org.mozilla.rocket";
    private static final String FENIX_RELEASE_PACKAGE_NAME = "org.mozilla.fenix";
    private static final String FENIX_BETA_PACKAGE_NAME = "org.mozilla.fenix.beta";
    private static final String FENIX_NIGHTLY_PACKAGE_NAME = "org.mozilla.fenix.nightly";
    private static final String FENNEC_RELEASE_VERSION = "68.7.0";
    private static final String FIREFOX_KLAR_VERSION = "8.2.0";
    private static final String FIREFOX_FOCUS_VERSION = "8.2.0";
    private static final String FIREFOX_LITE_VERSION = "2.1.13(19177)";
    private static final String FENIX_RELEASE_VERSION = "4.2.1";
    private static final String FENIX_BETA_VERSION = "Beta 200528 18:00";
    private static final String FENIX_NIGHTLY_VERSION = "Nightly 200530 06:01";

    private PackageManager packageManager;

    private static PackageInfo createPackageInfo(String versionName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = versionName;
        return packageInfo;
    }

    @Before
    public void setUp() {
        packageManager = mock(PackageManager.class);
    }

    @Test
    public void getVersionName_appInstalled_returnVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENNEC_RELEASE_VERSION));
        assertEquals(FENNEC_RELEASE_VERSION, InstalledApps.getVersionName(packageManager, App.FENNEC_RELEASE));
    }

    @Test
    public void getVersionName_appNotInstalled_returnEmptyString() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertEquals("", InstalledApps.getVersionName(packageManager, App.FENNEC_RELEASE));
    }

    @Test
    public void getVersionName_fennecRelease_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENNEC_RELEASE_VERSION));
        assertEquals(FENNEC_RELEASE_VERSION, InstalledApps.getVersionName(packageManager, App.FENNEC_RELEASE));
    }

    @Test
    public void getVersionName_firefoxKlar_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        assertEquals(FIREFOX_KLAR_VERSION, InstalledApps.getVersionName(packageManager, App.FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocus_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        assertEquals(FIREFOX_FOCUS_VERSION, InstalledApps.getVersionName(packageManager, App.FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLight_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        assertEquals(FIREFOX_LITE_VERSION, InstalledApps.getVersionName(packageManager, App.FIREFOX_LITE));
    }

    @Test
    public void getVersionName_fenix_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        assertEquals(FENIX_RELEASE_VERSION, InstalledApps.getVersionName(packageManager, App.FENIX_RELEASE));
    }

    @Test
    public void getVersionName_fennecReleaseInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENNEC_RELEASE_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FENNEC_RELEASE));
    }

    @Test
    public void getVersionName_firefoxKlarInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocusInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLightInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FIREFOX_LITE));
    }

    @Test
    public void getVersionName_fenixInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FENIX_RELEASE));
    }

    @Test
    public void getVersionName_fennecReleaseNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FENNEC_RELEASE));
    }

    @Test
    public void getVersionName_firefoxKlarNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocusNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLightNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FIREFOX_LITE));
    }

    @Test
    public void getVersionName_fenixNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FENIX_RELEASE));
    }

    @Test
    public void getInstalledApps_noneInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertTrue(InstalledApps.getInstalledApps(packageManager).isEmpty());
    }

    @Test
    public void getInstalledApps_someInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertThat(InstalledApps.getInstalledApps(packageManager), containsInAnyOrder(App.FIREFOX_KLAR, App.FIREFOX_LITE, App.FENIX_BETA));
    }

    @Test
    public void getInstalledApps_allInstalled_returnListWithAllApps() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENNEC_RELEASE_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        assertThat(InstalledApps.getInstalledApps(packageManager), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_noneInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertThat(InstalledApps.getNotInstalledApps(packageManager), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_someInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        assertThat(InstalledApps.getNotInstalledApps(packageManager), containsInAnyOrder(App.FENNEC_RELEASE, App.FIREFOX_FOCUS, App.FENIX_RELEASE));
    }

    @Test
    public void getNotInstalledApps_allInstalled_returnListWithAllApps() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENNEC_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENNEC_RELEASE_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        assertTrue(InstalledApps.getNotInstalledApps(packageManager).isEmpty());
    }
}