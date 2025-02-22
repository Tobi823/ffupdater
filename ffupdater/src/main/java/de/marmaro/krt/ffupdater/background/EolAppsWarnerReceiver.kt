package de.marmaro.krt.ffupdater.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.notification.NotificationBuilder
import de.marmaro.krt.ffupdater.security.PackageManagerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Keep
// TODO remove Bromite, Bromite SystemWebView, Lockwise and
class EolAppsWarnerReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent) {
        if (context == null || intent.action != Intent.ACTION_MY_PACKAGE_REPLACED || intent.`package` != BuildConfig.APPLICATION_ID) {
            return
        }
        showNotificationIfEolAppsAreInstalled(context)
    }

    private fun showNotificationIfEolAppsAreInstalled(context: Context) {
        scope.launch(Dispatchers.IO) {
            val eolApps = findInstalledSupportedEolApps(context) + findInstalledUnsupportedEolApps(context)
            if (eolApps.isNotEmpty()) {
                NotificationBuilder.showEolAppsNotification(context.applicationContext, eolApps)
            }
        }
    }

    private suspend fun findInstalledSupportedEolApps(context: Context): List<String> {
        return InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(context.applicationContext)
            .map { it.findImpl() }.filter { it.isEol() }.map { context.getString(it.title) }
    }

    private suspend fun findInstalledUnsupportedEolApps(context: Context): List<String> {
        return noLongerSupportedApps.filter { PackageManagerUtil.isAppInstalled(context.packageManager, it.value) }
            .map { it.key }
    }

    companion object {
        val noLongerSupportedApps = mapOf(
            "Bromite" to "org.bromite.bromite",
            "Bromite SystemWebView" to "org.bromite.webview",
            "Lockwise" to "mozilla.lockbox",
            "UngoogledChromium" to "org.ungoogled.chromium.stable",
            "Mulch" to "us.spotco.mulch",
            "Mulch (SystemWebView)" to "us.spotco.mulch_wv",
            "Mull" to "us.spotco.fennec_dos",
            "Kiwi" to "com.kiwibrowser.browser"
        )
    }
}