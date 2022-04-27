package de.marmaro.krt.ffupdater.installer

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

class ForegroundSessionInstaller(
    context: Context,
    app: App,
    file: File
) : ForegroundAppInstaller, SessionInstallerBase(context, app, file) {

    override suspend fun uncheckInstallAsync(context: Context): Deferred<AppInstaller.InstallResult> {
        return withContext(Dispatchers.Main) {
            async {
                registerIntentReceiver(context)
                try {
                    super.uncheckInstallAsync(context).await()
                } finally {
                    unregisterIntentReceiver(context)
                }
            }
        }
    }

    override fun getIntentNameForAppInstallationCallback(): String {
        return "de.marmaro.krt.ffupdater.installer.ForegroundSessionInstaller.app_installed"
    }

    override fun requestInstallationPermission(context: Context, bundle: Bundle) {
        try {
            val nestedIntent = bundle.get(Intent.EXTRA_INTENT) as Intent
            val sessionId = nestedIntent.extras!!.getInt("android.content.pm.extra.SESSION_ID")

            // don't use the nestedIntent to prevent a UnsafeIntentLaunchViolation warning
            val requestIntent = Intent("android.content.pm.action.CONFIRM_INSTALL")
            requestIntent.`package` = "com.android.packageinstaller"
            requestIntent.putExtra("android.content.pm.extra.SESSION_ID", sessionId)
            val flags = if (DeviceSdkTester.supportsAndroid12()) {
                PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 200, requestIntent, flags)
            pendingIntent.send()
        } catch (e: ActivityNotFoundException) {
            failure(-99, e.message ?: "/")
        }
    }
}