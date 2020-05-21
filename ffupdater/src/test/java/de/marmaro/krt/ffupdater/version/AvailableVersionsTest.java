package de.marmaro.krt.ffupdater.version;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.SimpleSharedPreferences;
import de.marmaro.krt.ffupdater.device.DeviceABI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 04.05.2020.
 */
public class AvailableVersionsTest {
    private PackageManager packageManager;
    private SharedPreferences sharedPreferences;
    private AvailableVersions availableVersions;

    private static PackageInfo createPackageInfo(String versionName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = versionName;
        return packageInfo;
    }

    @Before
    public void setUp() {
        packageManager = mock(PackageManager.class);
        sharedPreferences = new SimpleSharedPreferences();
        DeviceABI deviceABI = mock(DeviceABI.class);

        when(deviceABI.getBestSuitedAbi()).thenReturn(DeviceABI.ABI.ARM);
        when(deviceABI.isSdkIntEqualOrHigher(anyInt())).thenReturn(true);

        availableVersions = new AvailableVersions(packageManager, sharedPreferences, deviceABI);
    }

    @Test
    public void isUpdateAvailable_fennecRelease_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FENNEC_RELEASE;
        sharedPreferences.edit().putString("download_metadata_FENNEC_RELEASE_version_name", "68.8.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("68.8.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_fennecRelease_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FENNEC_RELEASE;
        sharedPreferences.edit().putString("download_metadata_FENNEC_RELEASE_version_name", "68.8.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("68.7.0"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxKlar_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_KLAR;
        sharedPreferences.edit().putString("download_metadata_FIREFOX_KLAR_version_name", "8.2.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.2.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxKlar_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_KLAR;
        sharedPreferences.edit().putString("download_metadata_FIREFOX_KLAR_version_name", "8.2.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.0.15"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxFocus_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_FOCUS;
        sharedPreferences.edit().putString("download_metadata_FIREFOX_FOCUS_version_name", "8.2.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.2.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxLite_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_FOCUS;
        sharedPreferences.edit().putString("download_metadata_FIREFOX_FOCUS_version_name", "8.2.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("8.0.15"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxLite_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_LITE;
        sharedPreferences.edit().putString("download_metadata_FIREFOX_LITE_version_name", "2.1.15").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("2.1.15(19177)"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_firefoxFocus_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FIREFOX_LITE;
        sharedPreferences.edit().putString("download_metadata_FIREFOX_LITE_version_name", "2.1.15").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("2.1.13(19177)"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_fenix_latestVersion_returnFalse() throws PackageManager.NameNotFoundException {
        App app = App.FENIX;
        sharedPreferences.edit().putString("download_metadata_FENIX_version_name", "4.3.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("4.3.0"));
        assertFalse(availableVersions.isUpdateAvailable(app));
    }

    @Test
    public void isUpdateAvailable_fenix_previousVersion_returnTrue() throws PackageManager.NameNotFoundException {
        App app = App.FENIX;
        sharedPreferences.edit().putString("download_metadata_FENIX_version_name", "4.3.0").apply();
        when(packageManager.getPackageInfo(app.getPackageName(), 0)).thenReturn(createPackageInfo("4.2.0"));
        assertTrue(availableVersions.isUpdateAvailable(app));
    }
}