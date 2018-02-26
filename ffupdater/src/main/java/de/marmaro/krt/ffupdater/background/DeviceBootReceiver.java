package de.marmaro.krt.ffupdater.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * After the boot process is finish, this class will start the {@link UpdateNotifierService}.
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != intent && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            onDeviceBoot(context, intent);
        }
    }

    protected void onDeviceBoot(Context context, Intent intent) {
        Log.i(TAG, "receive a device boot. Look up latest firefox version and register the repeating intent.");

        RepeatedNotifierExecuting.register(context);

        Intent checkVersions = new Intent(context, UpdateNotifierService.class);
        context.startService(checkVersions);
    }
}
