package de.marmaro.krt.ffupdater.version;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.SimpleSharedPreferences;
import de.marmaro.krt.ffupdater.device.DeviceABI;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by Tobiwan on 03.05.2020.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({Build.VERSION.class, DeviceABI.class})
public class AvailableVersionsIT {
    private AvailableVersions availableVersions;

    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", 29);

        PowerMockito.mockStatic(DeviceABI.class);
        when(DeviceABI.getBestSuitedAbi()).thenReturn(DeviceABI.ABI.ARM);

        PackageManager packageManager = mock(PackageManager.class);
        SharedPreferences sharedPreferences = new SimpleSharedPreferences();
        availableVersions = new AvailableVersions(packageManager, sharedPreferences);
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
        App app = App.FENIX;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }
}