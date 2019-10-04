package de.marmaro.krt.ffupdater.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * After the phone is booted, this class will start the {@link UpdateChecker}.
 * Reason: FFUpdater should be able to check for updates even after a phone reboot.
 */
public class AutoStartUpdateChecker extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            UpdateChecker.registerOrUnregister(context);
        }
    }
}
