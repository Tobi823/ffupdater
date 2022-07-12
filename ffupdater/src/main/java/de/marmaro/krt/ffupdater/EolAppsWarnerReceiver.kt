package de.marmaro.krt.ffupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.utils.ifTrue


class EolAppsWarnerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (context == null ||
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.`package` != BuildConfig.APPLICATION_ID
        ) {
            return
        }

        App.values()
            .filter { app -> app.impl.isEol() }
            .any { app -> app.impl.isInstalled(context) }
            .ifTrue { BackgroundNotificationBuilder.showEolAppsWarning(context) }
    }
}