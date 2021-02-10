package de.marmaro.krt.ffupdater.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * After the phone is booted, this class will start the [BackgroundUpdateChecker].
 * Reason: FFUpdater should be able to check for updates even after a phone reboot.
 */
class Autostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            BackgroundUpdateChecker.startOrStopBackgroundUpdateCheck(context)
        }
    }
}