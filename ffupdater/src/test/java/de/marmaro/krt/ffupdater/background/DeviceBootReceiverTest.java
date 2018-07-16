package de.marmaro.krt.ffupdater.background;

import android.app.Application;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by Tobiwan on 05.02.2018.
 */
@RunWith(RobolectricTestRunner.class)
public class DeviceBootReceiverTest {

	@Test
	public void onReceive_callUpdateNotifierService() throws Exception {
		Application application = RuntimeEnvironment.application;
		Intent expectedService = new Intent(application, UpdateNotifierService.class);

		DeviceBootReceiver deviceBootReceiver = new DeviceBootReceiver();
		deviceBootReceiver.onReceive(application, new Intent(Intent.ACTION_BOOT_COMPLETED));

		Intent serviceIntent = shadowOf(application).getNextStartedService();
		assertNotNull(serviceIntent);
		assertEquals(serviceIntent.getComponent(), expectedService.getComponent());
	}
}