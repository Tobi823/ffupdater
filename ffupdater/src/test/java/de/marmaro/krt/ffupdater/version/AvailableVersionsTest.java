package de.marmaro.krt.ffupdater.version;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Map;

import de.marmaro.krt.ffupdater.App;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 04.05.2020.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AvailableVersions.class})
public class AvailableVersionsTest {
    private PackageManager packageManager;
    private AvailableVersions availableVersions;
    private Map<App, String> versions;

    private static PackageInfo createPackageInfo(String versionName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = versionName;
        return packageInfo;
    }

    @Before
    public void setUp() {
        packageManager = mock(PackageManager.class);
        availableVersions = new AvailableVersions(packageManager);
        versions = Whitebox.getInternalState(availableVersions, "versions");
        versions.clear();
    }

    @Test
    public void isUpdateAvailable_fennecRelease_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FENNEC_RELEASE;
        versions.put(app, "68.8.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("68.8.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_fennecRelease_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FENNEC_RELEASE;
        versions.put(app, "68.8.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("68.7.0"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxKlar_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_KLAR;
        versions.put(app, "8.2.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.2.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxKlar_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_KLAR;
        versions.put(app, "8.2.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.0.15"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxFocus_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_FOCUS;
        versions.put(app, "8.2.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.2.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxLite_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_FOCUS;
        versions.put(app, "8.2.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.0.15"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxLite_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_LITE;
        versions.put(app, "2.1.15");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("2.1.15(19177)"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxFocus_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_LITE;
        versions.put(app, "2.1.15");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("2.1.13(19177)"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_fenix_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FENIX;
        versions.put(app, "4.3.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("4.3.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_fenix_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FENIX;
        versions.put(app, "4.3.0");
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("4.2.0"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }
}