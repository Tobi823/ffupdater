package de.marmaro.krt.ffupdater.version;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.SimpleSharedPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 21.05.2020.
 */
public class MetadataCacheTest {

    private MetadataCache metadataCache;
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        sharedPreferences = mock(SharedPreferences.class);
        metadataCache = new MetadataCache(sharedPreferences);
    }

    @Test
    public void getVersionName_withData_returnEmptyString() {
        when(sharedPreferences.getString("download_metadata_FENIX_version_name", "")).thenReturn("37.27.64");
        assertEquals("37.27.64", metadataCache.getVersionName(App.FENIX_RELEASE));
    }

    @Test
    public void getDownloadUrl_withData_returnEmptyString() {
        when(sharedPreferences.getString("download_metadata_FENIX_download_url", "")).thenReturn("https://something.com/index/app.apk");
        assertEquals("https://something.com/index/app.apk", metadataCache.getDownloadUrl(App.FENIX_RELEASE));
    }

    @Test
    public void isTimestampTooOld_withNewTimestamp_returnFalse() {
        when(sharedPreferences.getLong("download_metadata_FENIX_timestamp", -1)).thenReturn(System.currentTimeMillis());
        assertFalse(metadataCache.isTimestampTooOld(App.FENIX_RELEASE));
    }

    @Test
    public void isTimestampTooOld_withOldTimestamp_returnFalse() {
        when(sharedPreferences.getLong("download_metadata_FENIX_timestamp", -1)).thenReturn(946681200L); //01.01.2000 00:00:00
        assertTrue(metadataCache.isTimestampTooOld(App.FENIX_RELEASE));
    }

    @Test
    public void setMetadata_withAllData_setAllData() {
        SimpleSharedPreferences simpleSharedPreferences = new SimpleSharedPreferences();
        MetadataCache localMetadataCache = new MetadataCache(simpleSharedPreferences);
        localMetadataCache.updateAvailableVersionAndDownloadUrl(App.FENNEC_RELEASE, "14.12", "http://some.where/here.apk");

        assertEquals("14.12", localMetadataCache.getVersionName(App.FENNEC_RELEASE));
        assertEquals("http://some.where/here.apk", localMetadataCache.getDownloadUrl(App.FENNEC_RELEASE));
        assertFalse(localMetadataCache.isTimestampTooOld(App.FENNEC_RELEASE));
    }
}