package de.marmaro.krt.ffupdater.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class DeviceBootReceiverTest {
	DeviceBootReceiver sut;
	Context context;
	Intent intent;
	AlarmManager alarmManager;

	@Before
	public void setUp() {
		sut = spy(new DeviceBootReceiver());
		context = mock(Context.class);
		intent = mock(Intent.class);
		alarmManager = mock(AlarmManager.class);

		when(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager);
	}

	@Test
	public void onReceive_witthActionBootCompleted_registerRepeatedTimerAndStartService() throws Exception {
		when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);

		sut.onReceive(context, intent);

		// verify that UpdateNotifierService was called for update checking
		verify(context).startService(any(Intent.class));

		// verify that the repeated job was started
		verify(alarmManager).setInexactRepeating(eq(AlarmManager.ELAPSED_REALTIME), anyLong(), anyLong(), any(PendingIntent.class));
	}
}