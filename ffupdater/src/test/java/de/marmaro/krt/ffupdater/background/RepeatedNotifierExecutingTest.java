package de.marmaro.krt.ffupdater.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 05.02.2018.
 */
public class RepeatedNotifierExecutingTest {
//	RepeatedNotifierExecuting sut;
	Context context;
	AlarmManager alarmManager;

	@Before
	public void setUp() {
//		sut = spy(new RepeatedNotifierExecuting());
		context = mock(Context.class);
		alarmManager = mock(AlarmManager.class);

		when(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager);
	}

	@Test
	public void register_withFakeContext_checkIfAlarmManagerWasCalled() throws Exception {
		//sut.register(context);
		// TODO: fix

		// verify that the repeated job was started
//		ArgumentCaptor<Long> nextExecutionCaptor = ArgumentCaptor.forClass(Long.class);
//		verify(alarmManager).setInexactRepeating(eq(AlarmManager.ELAPSED_REALTIME),
//				nextExecutionCaptor.capture(), eq(RepeatedNotifierExecuting.INTERVAL_DURATION), any(PendingIntent.class));
//
//		long actual = nextExecutionCaptor.getValue();
//		long expected = SystemClock.elapsedRealtime() + RepeatedNotifierExecuting.INTERVAL_DURATION;
//		long diff = actual - expected;
		// the diff must be smaller than 10ms between actual and expected
//		assertTrue("The difference between actual and expected was " + diff, Math.abs(diff) < 10);
	}

}