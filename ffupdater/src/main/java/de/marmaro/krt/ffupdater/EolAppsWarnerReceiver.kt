package de.marmaro.krt.ffupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.app.EolApp
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder


class EolAppsWarnerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        require(intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
        require(intent.`package` == BuildConfig.APPLICATION_ID)
        requireNotNull(context)

        val eolAppsInstalled = EolApp.values()
            .any { it.impl.isInstalled(context) }
        if (eolAppsInstalled) {
            BackgroundNotificationBuilder.showEolAppsWarning(context)
        }
    }
}