package de.marmaro.krt.ffupdater.installer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import java.io.File

@RequiresApi(Build.VERSION_CODES.N)
class ForegroundSessionInstaller(
    private val context: Context,
    app: App,
    file: File
) : ForegroundAppInstaller, SessionInstallerBase(context, app, file) {

    override fun getIntentNameForAppInstallationCallback(): String {
        return "de.marmaro.krt.ffupdater.installer.ForegroundSessionInstaller.app_installed"
    }

    override fun requestInstallationPermission(bundle: Bundle) {
        try {
            val requestPermission = bundle.get(Intent.EXTRA_INTENT) as Intent
            context.startActivity(requestPermission)
        } catch (e: ActivityNotFoundException) {
            val tip = context.getString(R.string.install_activity__try_disable_miui_optimization)
            failure(-99, "${e.message}\n\n$tip")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        registerIntentReceiver()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        unregisterIntentReceiver()
    }
}