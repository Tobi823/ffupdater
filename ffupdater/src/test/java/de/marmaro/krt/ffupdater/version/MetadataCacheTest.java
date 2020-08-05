package de.marmaro.krt.ffupdater.version;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import de.marmaro.krt.ffupdater.SimpleSharedPreferences;

import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
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
    public void getVersionName_withData_returnCache() {
        when(sharedPreferences.getString("download_metadata_FIREFOX_LITE_version_name", "")).thenReturn("37.27.64");
        assertEquals("37.27.64", metadataCache.getVersionName(FIREFOX_LITE));
    }

    @Test
    public void getDownloadUrl_withData_returnCache() {
        when(sharedPreferences.getString("download_metadata_FIREFOX_LITE_download_url", "")).thenReturn("https://something.com/index/app.apk");
        assertEquals("https://something.com/index/app.apk", metadataCache.getDownloadUrl(FIREFOX_LITE));
    }

    @Test
    public void isTimestampTooOld_withNewTimestamp_returnFalse() {
        when(sharedPreferences.getLong("download_metadata_FIREFOX_LITE_timestamp", -1)).thenReturn(System.currentTimeMillis());
        assertFalse(metadataCache.isTimestampTooOld(FIREFOX_LITE));
    }

    @Test
    public void isTimestampTooOld_withOldTimestamp_returnFalse() {
        when(sharedPreferences.getLong("download_metadata_FIREFOX_LITE_timestamp", -1)).thenReturn(946681200L); //01.01.2000 00:00:00
        assertTrue(metadataCache.isTimestampTooOld(FIREFOX_LITE));
    }

    @Test
    public void setMetadata_withAllData_setAllData() {
        SimpleSharedPreferences simpleSharedPreferences = new SimpleSharedPreferences();
        MetadataCache localMetadataCache = new MetadataCache(simpleSharedPreferences);
        localMetadataCache.updateAvailableVersionAndDownloadUrl(FIREFOX_LITE, "14.12", "http://some.where/here.apk");

        assertEquals("14.12", localMetadataCache.getVersionName(FIREFOX_LITE));
        assertEquals("http://some.where/here.apk", localMetadataCache.getDownloadUrl(FIREFOX_LITE));
        assertFalse(localMetadataCache.isTimestampTooOld(FIREFOX_LITE));
    }
}