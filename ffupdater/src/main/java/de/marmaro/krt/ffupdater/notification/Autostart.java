package de.marmaro.krt.ffupdater.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.ACTION_BOOT_COMPLETED;

/**
 * After the phone is booted, this class will start the {@link BackgroundUpdateChecker}.
 * Reason: FFUpdater should be able to check for updates even after a phone reboot.
 */
public class Autostart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            new BackgroundUpdateCheckerCreator(context).startOrStopBackgroundUpdateCheck();
        }
    }
}
