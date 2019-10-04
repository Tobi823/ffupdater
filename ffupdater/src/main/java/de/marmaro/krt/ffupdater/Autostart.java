package de.marmaro.krt.ffupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.marmaro.krt.ffupdater.NotificationCreator;

/**
 * After the phone is booted, this class will start the {@link NotificationCreator}.
 * Reason: FFUpdater should be able to check for updates even after a phone reboot.
 */
public class Autostart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationCreator.registerOrUnregister(context);
        }
    }
}
