package de.marmaro.krt.ffupdater.installer.impl

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.ForegroundAppInstaller
import java.io.File

class ForegroundSessionInstaller(
    context: Context,
    app: App,
    file: File
) : ForegroundAppInstaller, AbstractSessionInstaller(context, app, file) {

    override val intentNameForAppInstallationCallback =
        "de.marmaro.krt.ffupdater.installer.impl.ForegroundSessionInstaller.app_installed"

    override fun requestInstallationPermission(context: Context, bundle: Bundle) {
        try {
            // ignore UnsafeIntentLaunchViolation because at least OnePlus needs this exact intent
            val requestPermission = bundle.get(Intent.EXTRA_INTENT) as Intent
            context.startActivity(requestPermission)
        } catch (e: ActivityNotFoundException) {
            failure(-99, e.message ?: "/")
        }
    }
}