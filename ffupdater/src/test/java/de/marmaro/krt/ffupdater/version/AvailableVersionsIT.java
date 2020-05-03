package de.marmaro.krt.ffupdater.version;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.marmaro.krt.ffupdater.App;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * Created by Tobiwan on 03.05.2020.
 */
public class AvailableVersionsIT {
    private int oldSdkInt;
    private String[] oldSupportedAbis;
    private PackageManager packageManager;
    private AvailableVersions availableVersions;

    @Before
    public void setUp() throws Exception {
        oldSdkInt = Build.VERSION.SDK_INT;
        oldSupportedAbis = android.os.Build.SUPPORTED_ABIS;

        setField(Build.VERSION.class.getField("SDK_INT"), 29);
        setField(android.os.Build.class.getField("SUPPORTED_ABIS"), new String[]{"armeabi-v7a"});

        packageManager = mock(PackageManager.class);
        availableVersions = new AvailableVersions(packageManager);
    }

    @After
    public void tearDown() throws Exception {
        setField(Build.VERSION.class.getField("SDK_INT"), oldSdkInt);
        setField(android.os.Build.class.getField("SUPPORTED_ABIS"), oldSupportedAbis);
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
    public void checkUpdateForApp_withFennecBeta_versionAndDownloadNotEmpty() {
        App app = App.FENNEC_BETA;
        availableVersions.checkUpdateForApp(app, null, null);

        assertFalse(availableVersions.getAvailableVersion(app).isEmpty());
        assertFalse(availableVersions.getDownloadUrl(app).isEmpty());
        System.out.printf("%s - version: %s url: %s\n", app.toString(), availableVersions.getAvailableVersion(app), availableVersions.getDownloadUrl(app));
    }

    @Test
    public void checkUpdateForApp_withFennecNightly_versionAndDownloadNotEmpty() {
        App app = App.FENNEC_NIGHTLY;
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

    /**
     * // https://stackoverflow.com/a/3301720
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void setField(Field field, Object value) throws NoSuchFieldException, IllegalAccessException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, value);
    }
}