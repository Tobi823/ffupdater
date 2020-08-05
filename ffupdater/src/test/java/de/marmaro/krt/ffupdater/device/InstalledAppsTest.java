package de.marmaro.krt.ffupdater.device;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;

import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.LOCKWISE;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstalledAppsTest {
    private static final String FIREFOX_KLAR_PACKAGE_NAME = "org.mozilla.klar";
    private static final String FIREFOX_FOCUS_PACKAGE_NAME = "org.mozilla.focus";
    private static final String FIREFOX_LITE_PACKAGE_NAME = "org.mozilla.rocket";
    private static final String FIREFOX_RELEASE_PACKAGE_NAME = "org.mozilla.firefox";
    private static final String FIREFOX_BETA_PACKAGE_NAME = "org.mozilla.firefox_beta";
    private static final String FIREFOX_NIGHTLY_PACKAGE_NAME = "org.mozilla.fenix";
    private static final String LOCKWISE_PACKAGE_NAME = "mozilla.lockbox";

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
    public void getVersionName_firefoxKlar_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "1";
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocus_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "2";
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLight_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "3";
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, FIREFOX_LITE));
    }

    @Test
    public void getVersionName_firefoxRelease_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "4";
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, FIREFOX_RELEASE));
    }

    @Test
    public void getVersionName_firefoxBeta_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "5";
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, FIREFOX_BETA));
    }

    @Test
    public void getVersionName_firefoxNightly_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "6";
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, App.FIREFOX_NIGHTLY));
    }

    @Test
    public void getVersionName_lockwise_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        String version = "7";
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(version));
        assertEquals(version, InstalledApps.getVersionName(packageManager, LOCKWISE));
    }

    @Test
    public void getVersionName_firefoxKlarInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("8"));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocusInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("9"));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLightInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("10"));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_LITE));
    }

    @Test
    public void getVersionName_firefoxReleaseInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("11"));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_RELEASE));
    }

    @Test
    public void getVersionName_firefoxBetaInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("12"));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_BETA));
    }

    @Test
    public void getVersionName_firefoxNigthlyInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("13"));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FIREFOX_NIGHTLY));
    }

    @Test
    public void getVersionName_lockwiseInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("14"));
        assertTrue(InstalledApps.isInstalled(packageManager, LOCKWISE));
    }

    @Test
    public void getVersionName_firefoxKlarNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocusNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLightNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FIREFOX_LITE));
    }

    @Test
    public void getVersionName_firefoxReleaseNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FIREFOX_RELEASE));
    }

    @Test
    public void getVersionName_firefoxBetaNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FIREFOX_BETA));
    }

    @Test
    public void getVersionName_firefoxNightlyInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FIREFOX_NIGHTLY));
    }

    @Test
    public void getVersionName_lockwiseNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, LOCKWISE));
    }

    @Test
    public void getInstalledApps_noneInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertTrue(InstalledApps.getInstalledApps(packageManager).isEmpty());
    }

    @Test
    public void getInstalledApps_someInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("15"));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("16"));
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("17"));
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("18"));
        assertThat(InstalledApps.getInstalledApps(packageManager), containsInAnyOrder(FIREFOX_KLAR, FIREFOX_LITE, FIREFOX_BETA, LOCKWISE));
    }

    @Test
    public void getInstalledApps_allInstalled_returnListWithAllApps() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("19"));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("20"));
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("21"));
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("22"));
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("23"));
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("24"));
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("25"));
        assertThat(InstalledApps.getInstalledApps(packageManager), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_noneInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertThat(InstalledApps.getNotInstalledApps(packageManager), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_someInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("26"));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("27"));
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("28"));
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("29"));
        assertThat(InstalledApps.getNotInstalledApps(packageManager), containsInAnyOrder(FIREFOX_FOCUS, FIREFOX_RELEASE, FIREFOX_NIGHTLY));
    }

    @Test
    public void getNotInstalledApps_allInstalled_returnListWithAllApps() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("30"));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("31"));
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("32"));
        when(packageManager.getPackageInfo(FIREFOX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("33"));
        when(packageManager.getPackageInfo(FIREFOX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("34"));
        when(packageManager.getPackageInfo(FIREFOX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("35"));
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo("36"));
        assertTrue(InstalledApps.getNotInstalledApps(packageManager).isEmpty());
    }
}