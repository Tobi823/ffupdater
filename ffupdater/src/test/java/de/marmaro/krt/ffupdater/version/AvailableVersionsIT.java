package de.marmaro.krt.ffupdater.version;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.SimpleSharedPreferences;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 03.05.2020.
 */
public class AvailableVersionsIT {
    private AvailableVersions availableVersions;

    @Before
    public void setUp() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);
        SharedPreferences sharedPreferences = new SimpleSharedPreferences();
        DeviceEnvironment deviceABI = mock(DeviceEnvironment.class);

        when(deviceABI.getBestSuitedAbi()).thenReturn(DeviceEnvironment.ABI.ARM);
        when(deviceABI.isSdkIntEqualOrHigher(anyInt())).thenReturn(true);

        availableVersions = new AvailableVersions(packageManager, sharedPreferences, deviceABI);
    }

    @Test
    public void checkUpdateForApp_withFennecRelease_versionAndDownloadNotEmpty() throws PackageManager.NameNotFoundException {
        App app = App.FENNEC_RELEASE;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }

    @Test
    public void checkUpdateForApp_withFirefoxKlar_versionAndDownloadNotEmpty() {
        App app = App.FIREFOX_KLAR;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }

    @Test
    public void checkUpdateForApp_withFirefoxFocus_versionAndDownloadNotEmpty() {
        App app = App.FIREFOX_FOCUS;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }

    @Test
    public void checkUpdateForApp_withFirefoxLite_versionAndDownloadNotEmpty() {
        App app = App.FIREFOX_LITE;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }

    @Test
    public void checkUpdateForApp_withFenix_versionAndDownloadNotEmpty() {
        App app = App.FENIX_RELEASE;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }
}