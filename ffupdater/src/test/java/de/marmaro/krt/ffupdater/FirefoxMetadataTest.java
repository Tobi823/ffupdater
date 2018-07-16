package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class FirefoxMetadataTest {
	PackageManager packageManager;
	PackageInfo packageInfo;

	@Before
	public void setUp() {
		packageManager = mock(PackageManager.class);
		packageInfo = mock(PackageInfo.class);
	}

	private void firefoxIsNotInstalled() throws Exception {
		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenThrow(new PackageManager.NameNotFoundException());
	}

	private void firefoxIsInstalled() throws Exception {
		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenReturn(packageInfo);
	}

	@Test
	public void isInstalled_firefoxIsNotInstalled_returnFalse() throws Exception {
		firefoxIsNotInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		assertEquals(false, metadata.isInstalled());
	}

	@Test
	public void isInstalled_firefoxIsInstalled_returnTrue() throws Exception {
		firefoxIsInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		assertEquals(true, metadata.isInstalled());
	}

	@Test
	public void getVersionCode_firefoxIsNotInstalled_returnFallback() throws Exception {
		firefoxIsNotInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		assertEquals(0, metadata.getVersionCode());
	}

	@Test
	public void getVersionCode_firefoxIsInstalled_returnVersionCode() throws Exception {
		firefoxIsInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		int versionCode = 2015538137;
		packageInfo.versionCode = versionCode;

		assertEquals(versionCode, metadata.getVersionCode());
	}

	@Test
	public void getVersionName_firefoxIsNotInstalled_returnFallback() throws Exception {
		firefoxIsNotInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		assertEquals("", metadata.getVersionName());
	}

	@Test
	public void getVersionName_firefoxIsInstalled_returnVersionName() throws Exception {
		firefoxIsInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		String versionName = "58.0.1";
		packageInfo.versionName = versionName;

		assertEquals(versionName, metadata.getVersionName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getVersionfirefoxIsNotInstalled_returnFallback() throws Exception {
		firefoxIsNotInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		metadata.getVersion();
	}

	@Test
	public void getVersionfirefoxIsInstalled_returnFallback() throws Exception {
		firefoxIsInstalled();
		FirefoxDetector metadata = new FirefoxDetector.Builder().checkLocalInstalledFirefox(packageManager);

		String versionName = "58.0.1";
		packageInfo.versionName = versionName;

		assertEquals(versionName, metadata.getVersion().get());
	}
}