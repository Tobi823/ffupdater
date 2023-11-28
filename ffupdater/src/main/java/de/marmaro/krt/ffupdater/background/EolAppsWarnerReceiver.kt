package de.marmaro.krt.ffupdater.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.notification.NotificationBuilder
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

        scope.launch(Dispatchers.IO) {
            val eolInstalled = InstalledAppsCache
                .getInstalledAppsWithCorrectFingerprint(context.applicationContext)
                .asSequence()
                .map { it.findImpl() }
                .filter { it.isEol() }
                .any()
            if (eolInstalled) {
                NotificationBuilder.showEolAppsNotification(context.applicationContext)
            }
        }
    }
}