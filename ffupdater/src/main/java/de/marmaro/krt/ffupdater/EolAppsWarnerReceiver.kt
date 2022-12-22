package de.marmaro.krt.ffupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import de.marmaro.krt.ffupdater.utils.ifTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class EolAppsWarnerReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent) {
        if (context == null ||
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.`package` != BuildConfig.APPLICATION_ID
        ) {
            return
        }

        scope.launch(Dispatchers.Default) {
            App.values()
                .filter { app -> app.impl.isEol() }
                .any { app -> app.impl.isInstalled(context) == InstallationStatus.INSTALLED }
                .ifTrue { BackgroundNotificationBuilder.INSTANCE.showEolAppsWarning(context) }
        }
    }
}