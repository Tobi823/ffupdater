package de.marmaro.krt.ffupdater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

	@Test
	public void getVersionCode_firefoxReleaseIsNotInstalled_returnFallback() throws Exception {
		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenThrow(new PackageManager.NameNotFoundException());
		when(packageManager.getPackageInfo("org.mozilla.firefox_beta", 0)).thenReturn(mock(PackageInfo.class));
		when(packageManager.getPackageInfo("org.mozilla.fennec_aurora", 0)).thenReturn(mock(PackageInfo.class));

		LocalInstalledVersions local = FirefoxDetector.create(packageManager).getLocalVersions();

		assertEquals(false, local.isPresent(UpdateChannel.RELEASE));
	}

	@Test
	public void getVersionCode_firefoxReleaseIsInstalled_returnVersionCode() throws Exception {
		PackageInfo packageInfo = mock(PackageInfo.class);
		String versionName = "61.1";
		int versionCode = 2015538140;
		packageInfo.versionName = versionName;
		packageInfo.versionCode = versionCode;

		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenReturn(packageInfo);
		when(packageManager.getPackageInfo("org.mozilla.firefox_beta", 0)).thenThrow(new PackageManager.NameNotFoundException());
		when(packageManager.getPackageInfo("org.mozilla.fennec_aurora", 0)).thenThrow(new PackageManager.NameNotFoundException());

		LocalInstalledVersions local = FirefoxDetector.create(packageManager).getLocalVersions();
		Version version = local.getVersionString(UpdateChannel.RELEASE);

		assertEquals(versionName, version.getName());
		assertEquals(versionCode, version.getCode());
	}

	@Test
	public void getVersionCode_firefoxBetaIsNotInstalled_returnFallback() throws Exception {
		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenReturn(mock(PackageInfo.class));
		when(packageManager.getPackageInfo("org.mozilla.firefox_beta", 0)).thenThrow(new PackageManager.NameNotFoundException());
		when(packageManager.getPackageInfo("org.mozilla.fennec_aurora", 0)).thenReturn(mock(PackageInfo.class));

		LocalInstalledVersions local = FirefoxDetector.create(packageManager).getLocalVersions();

		assertEquals(false, local.isPresent(UpdateChannel.BETA));
	}

	@Test
	public void getVersionCode_firefoxBetaIsInstalled_returnVersionCode() throws Exception {
		PackageInfo packageInfo = mock(PackageInfo.class);
		String versionName = "62.0b7";
		int versionCode = 2015569228;
		packageInfo.versionName = versionName;
		packageInfo.versionCode = versionCode;

		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenThrow(new PackageManager.NameNotFoundException());
		when(packageManager.getPackageInfo("org.mozilla.firefox_beta", 0)).thenReturn(packageInfo);
		when(packageManager.getPackageInfo("org.mozilla.fennec_aurora", 0)).thenThrow(new PackageManager.NameNotFoundException());

		LocalInstalledVersions local = FirefoxDetector.create(packageManager).getLocalVersions();
		Version version = local.getVersionString(UpdateChannel.BETA);

		assertEquals(versionName, version.getName());
		assertEquals(versionCode, version.getCode());
	}
	@Test
	public void getVersionCode_firefoxNightlyIsNotInstalled_returnFallback() throws Exception {
		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenReturn(mock(PackageInfo.class));
		when(packageManager.getPackageInfo("org.mozilla.firefox_beta", 0)).thenReturn(mock(PackageInfo.class));
		when(packageManager.getPackageInfo("org.mozilla.fennec_aurora", 0)).thenThrow(new PackageManager.NameNotFoundException());

		LocalInstalledVersions local = FirefoxDetector.create(packageManager).getLocalVersions();

		assertEquals(false, local.isPresent(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getVersionCode_firefoxNightlyIsInstalled_returnVersionCode() throws Exception {
		PackageInfo packageInfo = mock(PackageInfo.class);
		String versionName = "63.0a1";
		int versionCode = 2015569428;
		packageInfo.versionName = versionName;
		packageInfo.versionCode = versionCode;

		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenThrow(new PackageManager.NameNotFoundException());
		when(packageManager.getPackageInfo("org.mozilla.firefox_beta", 0)).thenThrow(new PackageManager.NameNotFoundException());
		when(packageManager.getPackageInfo("org.mozilla.fennec_aurora", 0)).thenReturn(packageInfo);

		LocalInstalledVersions local = FirefoxDetector.create(packageManager).getLocalVersions();
		Version version = local.getVersionString(UpdateChannel.NIGHTLY);

		assertEquals(versionName, version.getName());
		assertEquals(versionCode, version.getCode());
	}

}