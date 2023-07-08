package de.marmaro.krt.ffupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.notification.BackgroundNotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Keep
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
            val eolInstalled = App.values()
                .map { it.findImpl() }
                .filter { it.isEol() }
                .any { it.isInstalled(context.applicationContext) == InstallationStatus.INSTALLED }
            if (eolInstalled) {
                BackgroundNotificationBuilder.showEolAppsNotification(context.applicationContext)
            }
        }
    }
}