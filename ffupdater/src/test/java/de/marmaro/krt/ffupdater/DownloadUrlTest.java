package de.marmaro.krt.ffupdater;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class DownloadUrlTest {
	@Test
	public void isApiLevelSupported_withMinimumSupportedLevel_returnTrue() throws Exception {
		DownloadUrl downloadUrl = new DownloadUrl("arm64", 16);
		assertEquals(true, downloadUrl.isApiLevelSupported());
	}

	@Test
	public void isApiLevelSupported_withSupportedLevel_returnTrue() throws Exception {
		DownloadUrl downloadUrl = new DownloadUrl("arm", 17);
		assertEquals(true, downloadUrl.isApiLevelSupported());
	}

	@Test
	public void isApiLevelSupported_withUnsupportedLevel_returnTrue() throws Exception {
		DownloadUrl downloadUrl = new DownloadUrl("arm", 15);
		assertEquals(false, downloadUrl.isApiLevelSupported());
	}

	@Test
	public void getArchitecture() throws Exception {
		String architecture = "x86";
		DownloadUrl downloadUrl = new DownloadUrl(architecture, 15);
		assertEquals(architecture, downloadUrl.getArchitecture());
	}

	@Test
	public void getApiLevel() throws Exception {
		int apiLevel = 18;
		DownloadUrl downloadUrl = new DownloadUrl("x86_64", apiLevel);
		assertEquals(apiLevel, downloadUrl.getApiLevel());
	}

	@Test
	public void getUrl_forArm_returnDefaultUrl() throws Exception {
		DownloadUrl downloadUrl = new DownloadUrl("arm", 19);

		String expected ="https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi";
		assertEquals(expected, downloadUrl.getUrl());
	}

	@Test
	public void getUrl_forI686_returnX86Url() throws Exception {
		DownloadUrl downloadUrl = new DownloadUrl("i686", 19);

		String expected ="https://download.mozilla.org/?product=fennec-latest&os=android-x86&lang=multi";
		assertEquals(expected, downloadUrl.getUrl());
	}

	@Test
	public void getUrl_forX8664_returnX86Url() throws Exception {
		DownloadUrl downloadUrl = new DownloadUrl("x86_64", 19);

		String expected ="https://download.mozilla.org/?product=fennec-latest&os=android-x86&lang=multi";
		assertEquals(expected, downloadUrl.getUrl());
	}

}