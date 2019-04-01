package de.marmaro.krt.ffupdater.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * After the boot process is finish, this class will start the {@link UpdateNotifierService}.
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            UpdateChecker.register();
        }
    }
}
