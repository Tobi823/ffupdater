package de.marmaro.krt.ffupdater.background;

import android.content.Intent;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URL;

import de.marmaro.krt.ffupdater.Version;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class LatestReleaseServiceTest {
//	LatestReleaseService sut;

	@BeforeClass
	public static void setUpOnce() {
//		// register for faking network requests
//		try {
//			URL.setURLStreamHandlerFactory(new MozillaVersionsTest.MockURLStreamHandler());
//		} catch (Error error) {
//			// it is normal, than an error will be thrown when the method setURLStreamHandlerFactory is called a second time
//			if (!error.getMessage().equals("factory already defined")) {
//				throw error;
//			}
//		}
	}

	@Before
	public void setUp() {
//		sut = spy(new LatestReleaseService());
	}

	@Test
	public void onHandleIntent_withFakeNetworkConnection_broadcastVersion() throws Exception {
//		sut.onHandleIntent(mock(Intent.class));
//
//		ArgumentCaptor<Version> captor = ArgumentCaptor.forClass(Version.class);
//		verify(sut).broadcastVersion(captor.capture());
//
//		Version actual = captor.getValue();
//		assertEquals(MozillaVersionsTest.MozillaHttpsUrlConnection.FIREFOX_VERSION, actual.get());
	}

}