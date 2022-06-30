package de.marmaro.krt.ffupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.app.eol.EolApp
import de.marmaro.krt.ffupdater.background.BackgroundNotificationBuilder


class EolAppsWarnerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        require(intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
        require(intent.`package` == BuildConfig.APPLICATION_ID)
        requireNotNull(context)

        EolApp.values()
            .any { it.impl.isInstalled(context) }
            .let { BackgroundNotificationBuilder.showEolAppsWarning(context) }
    }
}