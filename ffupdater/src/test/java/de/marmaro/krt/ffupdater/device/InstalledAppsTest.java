package de.marmaro.krt.ffupdater.device;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;

import static de.marmaro.krt.ffupdater.App.FENIX_BETA;
import static de.marmaro.krt.ffupdater.App.FENIX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FENIX_RELEASE;
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
    private static final String FENIX_RELEASE_PACKAGE_NAME = "org.mozilla.fenix";
    private static final String FENIX_BETA_PACKAGE_NAME = "org.mozilla.fenix.beta";
    private static final String FENIX_NIGHTLY_PACKAGE_NAME = "org.mozilla.fenix.nightly";
    private static final String LOCKWISE_PACKAGE_NAME = "mozilla.lockbox";

    private static final String FIREFOX_KLAR_VERSION = "8.2.0";
    private static final String FIREFOX_FOCUS_VERSION = "8.2.0";
    private static final String FIREFOX_LITE_VERSION = "2.1.13(19177)";
    private static final String FENIX_RELEASE_VERSION = "4.2.1";
    private static final String FENIX_BETA_VERSION = "Beta 200528 18:00";
    private static final String FENIX_NIGHTLY_VERSION = "Nightly 200530 06:01";
    private static final String LOCKWISE_VERSION = "3.3.0";

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
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        assertEquals(FIREFOX_KLAR_VERSION, InstalledApps.getVersionName(packageManager, FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocus_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        assertEquals(FIREFOX_FOCUS_VERSION, InstalledApps.getVersionName(packageManager, FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLight_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        assertEquals(FIREFOX_LITE_VERSION, InstalledApps.getVersionName(packageManager, FIREFOX_LITE));
    }

    @Test
    public void getVersionName_fenixRelease_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        assertEquals(FENIX_RELEASE_VERSION, InstalledApps.getVersionName(packageManager, FENIX_RELEASE));
    }

    @Test
    public void getVersionName_fenixBeta_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        assertEquals(FENIX_BETA_VERSION, InstalledApps.getVersionName(packageManager, FENIX_BETA));
    }

    @Test
    public void getVersionName_fenixNightly_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        assertEquals(FENIX_NIGHTLY_VERSION, InstalledApps.getVersionName(packageManager, App.FENIX_NIGHTLY));
    }

    @Test
    public void getVersionName_lockwise_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(LOCKWISE_VERSION));
        assertEquals(LOCKWISE_VERSION, InstalledApps.getVersionName(packageManager, LOCKWISE));
    }

    @Test
    public void getVersionName_firefoxKlarInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_KLAR));
    }

    @Test
    public void getVersionName_firefoxFocusInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_FOCUS));
    }

    @Test
    public void getVersionName_firefoxLightInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, FIREFOX_LITE));
    }

    @Test
    public void getVersionName_fenixReleaseInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, FENIX_RELEASE));
    }

    @Test
    public void getVersionName_fenixBetaInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, FENIX_BETA));
    }

    @Test
    public void getVersionName_fenixNigthlyInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        assertTrue(InstalledApps.isInstalled(packageManager, App.FENIX_NIGHTLY));
    }

    @Test
    public void getVersionName_lockwiseInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(LOCKWISE_VERSION));
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
    public void getVersionName_fenixReleaseNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FENIX_RELEASE));
    }

    @Test
    public void getVersionName_fenixBetaNotInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, FENIX_BETA));
    }

    @Test
    public void getVersionName_fenixNightlyInstalled_returnCorrectVersion() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertFalse(InstalledApps.isInstalled(packageManager, App.FENIX_NIGHTLY));
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
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertTrue(InstalledApps.getInstalledApps(packageManager).isEmpty());
    }

    @Test
    public void getInstalledApps_someInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(LOCKWISE_VERSION));
        assertThat(InstalledApps.getInstalledApps(packageManager), containsInAnyOrder(FIREFOX_KLAR, FIREFOX_LITE, FENIX_BETA, LOCKWISE));
    }

    @Test
    public void getInstalledApps_allInstalled_returnListWithAllApps() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(LOCKWISE_VERSION));
        assertThat(InstalledApps.getInstalledApps(packageManager), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_noneInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        assertThat(InstalledApps.getNotInstalledApps(packageManager), containsInAnyOrder(App.values()));
    }

    @Test
    public void getNotInstalledApps_someInstalled_returnEmptyList() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(LOCKWISE_VERSION));
        assertThat(InstalledApps.getNotInstalledApps(packageManager), containsInAnyOrder(FIREFOX_FOCUS, FENIX_RELEASE, FENIX_NIGHTLY));
    }

    @Test
    public void getNotInstalledApps_allInstalled_returnListWithAllApps() throws PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo(FIREFOX_KLAR_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_KLAR_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_FOCUS_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_FOCUS_VERSION));
        when(packageManager.getPackageInfo(FIREFOX_LITE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FIREFOX_LITE_VERSION));
        when(packageManager.getPackageInfo(FENIX_RELEASE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_RELEASE_VERSION));
        when(packageManager.getPackageInfo(FENIX_BETA_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_BETA_VERSION));
        when(packageManager.getPackageInfo(FENIX_NIGHTLY_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(FENIX_NIGHTLY_VERSION));
        when(packageManager.getPackageInfo(LOCKWISE_PACKAGE_NAME, 0)).thenReturn(createPackageInfo(LOCKWISE_VERSION));
        assertTrue(InstalledApps.getNotInstalledApps(packageManager).isEmpty());
    }
}