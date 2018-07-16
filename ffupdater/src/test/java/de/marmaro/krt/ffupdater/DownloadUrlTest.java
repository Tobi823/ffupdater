package de.marmaro.krt.ffupdater;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import static de.marmaro.krt.ffupdater.DownloadUrl.PROPERTY_OS_ARCHITECTURE;
import static org.junit.Assert.assertEquals;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class DownloadUrlTest {

	String jsonResult;
	String nightlyVersion;
	String betaVersion;
	String releaseVersion;

	@Before
	public void setUp() {
		nightlyVersion = "63.0a1";
		betaVersion = "62.0b7";
		releaseVersion = "61.0";
		jsonResult = "{\n" +
				"  'nightly_version': '" + nightlyVersion + "',\n" +
				"  'beta_version': '" + betaVersion + "',\n" +
				"  'version': '" + releaseVersion + "'}";
	}

	@Test
	public void getUrl_releaseArm_returnDefaultUrl() throws Exception {
        System.setProperty(PROPERTY_OS_ARCHITECTURE, "arm");
		DownloadUrl downloadUrl = DownloadUrl.create();

		String expected = "https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.RELEASE));
	}

	@Test
	public void getUrl_releaseI686_returnX86Url() throws Exception {
        System.setProperty(PROPERTY_OS_ARCHITECTURE, "i686");
        DownloadUrl downloadUrl = DownloadUrl.create();

		String expected = "https://download.mozilla.org/?product=fennec-latest&os=android-x86&lang=multi";
        assertEquals(expected, downloadUrl.getUrl(UpdateChannel.RELEASE));
	}

	@Test
	public void getUrl_releaseX8664_returnX86Url() throws Exception {
        System.setProperty(PROPERTY_OS_ARCHITECTURE, "x86_64");
        DownloadUrl downloadUrl = DownloadUrl.create();

		String expected = "https://download.mozilla.org/?product=fennec-latest&os=android-x86&lang=multi";
        assertEquals(expected, downloadUrl.getUrl(UpdateChannel.RELEASE));
	}

	@Test
	public void getUrl_betaArm_returnDefaultUrl() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "arm");
		DownloadUrl downloadUrl = DownloadUrl.create();

		String expected = "https://download.mozilla.org/?product=fennec-beta-latest&os=android&lang=multi";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.BETA));
	}

	@Test
	public void getUrl_betaI686_returnX86Url() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "i686");
		DownloadUrl downloadUrl = DownloadUrl.create();

		String expected = "https://download.mozilla.org/?product=fennec-beta-latest&os=android-x86&lang=multi";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.BETA));
	}

	@Test
	public void getUrl_betaX8664_returnX86Url() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "x86_64");
		DownloadUrl downloadUrl = DownloadUrl.create();

		String expected = "https://download.mozilla.org/?product=fennec-beta-latest&os=android-x86&lang=multi";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.BETA));
	}

	@Test
	public void getUrl_nightlyArm_returnDefaultUrl() throws Exception {
		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

		System.setProperty(PROPERTY_OS_ARCHITECTURE, "arm");
		DownloadUrl downloadUrl = DownloadUrl.create(mobileVersions);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-63.0a1.multi.android-arm.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyI686_returnX86Url() throws Exception {
		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

		System.setProperty(PROPERTY_OS_ARCHITECTURE, "i686");
		DownloadUrl downloadUrl = DownloadUrl.create(mobileVersions);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyX8664_returnX86Url() throws Exception {
		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

		System.setProperty(PROPERTY_OS_ARCHITECTURE, "x86_64");
		DownloadUrl downloadUrl = DownloadUrl.create(mobileVersions);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}


	@Test
	public void getUrl_nightlyArmWithUpdate_returnDefaultUrl() throws Exception {
		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

		System.setProperty(PROPERTY_OS_ARCHITECTURE, "arm");
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(mobileVersions);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-63.0a1.multi.android-arm.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyI686WithUpdate_returnX86Url() throws Exception {
		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

		System.setProperty(PROPERTY_OS_ARCHITECTURE, "i686");
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(mobileVersions);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyX8664WithUpdate_returnX86Url() throws Exception {
		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(jsonResult, MobileVersions.class);

		System.setProperty(PROPERTY_OS_ARCHITECTURE, "x86_64");
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(mobileVersions);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

}