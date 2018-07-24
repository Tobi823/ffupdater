package de.marmaro.krt.ffupdater;

import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.marmaro.krt.ffupdater.github.Release;
import de.marmaro.krt.ffupdater.mozilla.MobileVersions;

import static de.marmaro.krt.ffupdater.DownloadUrl.PROPERTY_OS_ARCHITECTURE;
import static org.junit.Assert.assertEquals;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class DownloadUrlTest {

	String nightlyVersion;
	String betaVersion;
	String releaseVersion;
	ApiResponses apiResponses;

	@Before
	public void setUp() throws IOException {
		nightlyVersion = "63.0a1";
		betaVersion = "62.0b7";
		releaseVersion = "61.0";

		InputStream mobileVersionsStream = getClass().getClassLoader().getResourceAsStream("mobile_versions.json");
		String mobileVersionsResult = IOUtils.toString(mobileVersionsStream, StandardCharsets.UTF_8.name());

		GsonBuilder gsonBuilder = new GsonBuilder();
		MobileVersions mobileVersions = gsonBuilder.create().fromJson(mobileVersionsResult, MobileVersions.class);

		InputStream releasesStream = getClass().getClassLoader().getResourceAsStream("releases.json");
		String releasesResult = IOUtils.toString(releasesStream, StandardCharsets.UTF_8.name());

		GsonBuilder gsonBuilder2 = new GsonBuilder();
		Release release = gsonBuilder2.create().fromJson(releasesResult, Release.class);

		apiResponses = new ApiResponses(mobileVersions, release);
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
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "arm");
		DownloadUrl downloadUrl = DownloadUrl.create(apiResponses);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-63.0a1.multi.android-arm.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyI686_returnX86Url() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "i686");
		DownloadUrl downloadUrl = DownloadUrl.create(apiResponses);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyX8664_returnX86Url() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "x86_64");
		DownloadUrl downloadUrl = DownloadUrl.create(apiResponses);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}


	@Test
	public void getUrl_nightlyArmWithUpdate_returnDefaultUrl() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "arm");
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(apiResponses);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-api-16/fennec-63.0a1.multi.android-arm.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyI686WithUpdate_returnX86Url() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "i686");
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(apiResponses);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_nightlyX8664WithUpdate_returnX86Url() throws Exception {
		System.setProperty(PROPERTY_OS_ARCHITECTURE, "x86_64");
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(apiResponses);

		String expected = "https://archive.mozilla.org/pub/mobile/nightly/latest-mozilla-central-android-x86/fennec-63.0a1.multi.android-i386.apk";
		assertEquals(expected, downloadUrl.getUrl(UpdateChannel.NIGHTLY));
	}

	@Test
	public void getUrl_firefoxFocus_getUrl() {
		DownloadUrl downloadUrl = DownloadUrl.create(apiResponses);
		String actual = downloadUrl.getUrl(UpdateChannel.FOCUS);

		String expected = "https://github.com/mozilla-mobile/focus-android/releases/download/V6.0-RC5/Focus.apk";
		assertEquals(expected, actual);
	}

	@Test
	public void getUrl_firefoxFocusWithUpdate_getUrl() {
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(apiResponses);
		String actual = downloadUrl.getUrl(UpdateChannel.FOCUS);

		String expected = "https://github.com/mozilla-mobile/focus-android/releases/download/V6.0-RC5/Focus.apk";
		assertEquals(expected, actual);
	}
	@Test
	public void getUrl_firefoxKlar_getUrl() {
		DownloadUrl downloadUrl = DownloadUrl.create(apiResponses);
		String actual = downloadUrl.getUrl(UpdateChannel.KLAR);

		String expected = "https://github.com/mozilla-mobile/focus-android/releases/download/V6.0-RC5/Klar.apk";
		assertEquals(expected, actual);
	}

	@Test
	public void getUrl_firefoxKlarWithUpdate_getUrl() {
		DownloadUrl downloadUrl = DownloadUrl.create();
		downloadUrl.update(apiResponses);
		String actual = downloadUrl.getUrl(UpdateChannel.KLAR);

		String expected = "https://github.com/mozilla-mobile/focus-android/releases/download/V6.0-RC5/Klar.apk";
		assertEquals(expected, actual);
	}
}