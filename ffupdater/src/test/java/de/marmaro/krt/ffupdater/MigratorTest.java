package de.marmaro.krt.ffupdater;

import android.content.SharedPreferences;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MigratorTest {

    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        sharedPreferences = new SPMockBuilder().createSharedPreferences();
    }

    @Test
    public void migrate_newVersion_deleteMetadataCache() {
        sharedPreferences.edit().putString("migrator_ffupdater_version_name", "65.0.0").commit();
        final Migrator migrator = new Migrator(sharedPreferences, "65.0.1");

        // metadata cache which must be deleted
        final String key = "download_metadata_FIREFOX_RELEASE_download_url";
        final String url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk";
        sharedPreferences.edit().putString(key, url).commit();

        // migrate and delete cache
        migrator.migrate();

        assertFalse(sharedPreferences.contains(key));
    }

    @Test
    public void migrate_currentVersion_keepMetadataCache() {
        sharedPreferences.edit().putString("migrator_ffupdater_version_name", "65.0.0").commit();
        final Migrator migrator = new Migrator(sharedPreferences, "65.0.0");

        // metadata cache which must be deleted
        final String key = "download_metadata_FIREFOX_RELEASE_download_url";
        final String url = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.release.latest.arm64-v8a/artifacts/public/build/arm64-v8a/target.apk";
        sharedPreferences.edit().putString(key, url).commit();

        // migrate and delete cache
        migrator.migrate();

        assertTrue(sharedPreferences.contains(key));
        assertEquals(url, sharedPreferences.getString(key, null));
    }
}