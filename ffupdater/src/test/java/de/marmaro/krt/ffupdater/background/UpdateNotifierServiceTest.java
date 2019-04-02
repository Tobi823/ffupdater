package de.marmaro.krt.ffupdater.background;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;


import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class UpdateNotifierServiceTest {

//	UpdateNotifierService updateNotifierService;

	@BeforeClass
	public static void setUpOnce() {
		// register for faking network requests
		try {
//			URL.setURLStreamHandlerFactory(new MozillaVersionsTest.MockURLStreamHandler());
		} catch (Error error) {
			// it is normal, than an error will be thrown when the method setURLStreamHandlerFactory is called a second time
			if (!error.getMessage().equals("factory already defined")) {
				throw error;
			}
		}
	}

	@Before
	public void setUp() {
//		updateNotifierService = spy(new UpdateNotifierService());
	}

	@Test
	public void onHandleIntent_withUpdateAvailable_showNotification() throws Exception {
//		// mock the methods to make this test simpler
//		doReturn(true).when(updateNotifierService).isUpdateAvailable();
//		doNothing().when(updateNotifierService).showNotification();
//
//		updateNotifierService.onHandleIntent(mock(Intent.class));
//
//		verify(updateNotifierService).showNotification();
	}

	@Test
	public void onHandleIntent_withNoUpdateAvailable_dontShowNotification() throws Exception {
//		// mock the methods to make this test simpler
//		doReturn(false).when(updateNotifierService).isUpdateAvailable();
//		doNothing().when(updateNotifierService).showNotification();
//
//		updateNotifierService.onHandleIntent(mock(Intent.class));
//
//		verify(updateNotifierService, never()).showNotification();
	}

	@Test
	public void isUpdateAvailable_withNewVersionAvailable_returnTrue() throws Exception {
//		// fake local installed app
//		PackageManager packageManager = mock(PackageManager.class);
//		when(updateNotifierService.getPackageManager()).thenReturn(packageManager);
//
//		PackageInfo packageInfo = mock(PackageInfo.class);
//		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenReturn(packageInfo);
//
//		// set an older version than the faked release (MozillaHttpsUrlConnection.FIREFOX_VERSION = 57.0.4)
//		packageInfo.versionName = "56.0.1";
//
//		assertEquals(true, updateNotifierService.isUpdateAvailable());
	}

	@Test
	public void isUpdateAvailable_withNoNewVersionAvailable_returnFalse() throws Exception {
//		// fake local installed app
//		PackageManager packageManager = mock(PackageManager.class);
//		when(updateNotifierService.getPackageManager()).thenReturn(packageManager);
//
//		PackageInfo packageInfo = mock(PackageInfo.class);
//		when(packageManager.getPackageInfo("org.mozilla.firefox", 0)).thenReturn(packageInfo);
//
//		// set an older version than the faked release (MozillaHttpsUrlConnection.FIREFOX_VERSION = 57.0.4)
//		packageInfo.versionName = MozillaVersionsTest.MozillaHttpsUrlConnection.FIREFOX_VERSION;
//
//		assertEquals(false, updateNotifierService.isUpdateAvailable());
	}
}