package de.marmaro.krt.ffupdater.background;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.AndroidJUnitRunner;
import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_SCREEN_ON;
import static org.junit.Assert.assertEquals;

/**
 * Created by Tobiwan on 05.02.2018.
 */
@RunWith(AndroidJUnit4.class)
public class DeviceBootReceiverTest extends AndroidJUnitRunner {
    Context context;
    Configuration config;
    DeviceBootReceiver deviceBootReceiver;

    @Before
    public void setUp() {
        // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/testing#java
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        config = new Configuration.Builder()
                // Set log level to Log.DEBUG to
                // make it easier to see why tests failed
                .setMinimumLoggingLevel(Log.DEBUG)
                // Use a SynchronousExecutor to make it easier to write tests
                .setExecutor(new SynchronousExecutor())
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        deviceBootReceiver = new DeviceBootReceiver();
    }

    @Test
    public void onReceive_withActionBootCompleted_registerWorkRequest() throws Exception {
        List<WorkInfo> preState = WorkManager.getInstance().getWorkInfosForUniqueWork(UpdateChecker.WORK_MANAGER_KEY).get();
        assertEquals(0, preState.size());

        deviceBootReceiver.onReceive(context, new Intent(ACTION_BOOT_COMPLETED));

        List<WorkInfo> postState = WorkManager.getInstance().getWorkInfosForUniqueWork(UpdateChecker.WORK_MANAGER_KEY).get();
        assertEquals(1, postState.size());
    }

    @Test
    public void onReceive_withoutActionBootCompleted_noRegisterWorkRequest() throws Exception {
        List<WorkInfo> preState = WorkManager.getInstance().getWorkInfosForUniqueWork(UpdateChecker.WORK_MANAGER_KEY).get();
        assertEquals(0, preState.size());

        deviceBootReceiver.onReceive(context, new Intent(ACTION_SCREEN_ON));

        List<WorkInfo> postState = WorkManager.getInstance().getWorkInfosForUniqueWork(UpdateChecker.WORK_MANAGER_KEY).get();
        assertEquals(0, postState.size());
    }

    @Test
    public void onReceive_withNull_noRegisterWorkRequest() throws Exception {
        List<WorkInfo> preState = WorkManager.getInstance().getWorkInfosForUniqueWork(UpdateChecker.WORK_MANAGER_KEY).get();
        assertEquals(0, preState.size());

        deviceBootReceiver.onReceive(context, new Intent(ACTION_SCREEN_ON));

        List<WorkInfo> postState = WorkManager.getInstance().getWorkInfosForUniqueWork(UpdateChecker.WORK_MANAGER_KEY).get();
        assertEquals(0, postState.size());
    }
}