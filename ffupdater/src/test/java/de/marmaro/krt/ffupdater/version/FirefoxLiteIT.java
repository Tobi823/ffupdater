package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;

import static org.junit.Assert.assertFalse;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FirefoxLiteIT {

    private static FirefoxLite firefoxLite;

    @BeforeClass
    public static void onlyOnce() {
        firefoxLite = FirefoxLite.findLatest();
    }

    @Test
    public void getVersion_withNoParams_returnNonEmptyString() {
        assertFalse(firefoxLite.getVersion().isEmpty());
        System.out.println(App.FIREFOX_LITE + " - version: " + firefoxLite.getVersion());
    }

    @Test
    public void getDownloadUrl_withNoParams_returnNonEmptyString() {
        assertFalse(firefoxLite.getDownloadUrl().isEmpty());
        System.out.println(App.FIREFOX_LITE + " - " + firefoxLite.getDownloadUrl());
    }
}