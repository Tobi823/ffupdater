package de.marmaro.krt.ffupdater.installer

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.MaintainedApp
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

@RequiresApi(Build.VERSION_CODES.N)
class BackgroundSessionInstaller(
    context: Context,
    app: MaintainedApp,
    file: File
) : BackgroundAppInstaller, SessionInstallerBase(context, app, file) {

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
        return "de.marmaro.krt.ffupdater.installer.BackgroundSessionInstaller.app_installed"
    }

    override fun requestInstallationPermission(context: Context, bundle: Bundle) {
        val status = bundle.getInt(PackageInstaller.EXTRA_STATUS)
        failure(status, context.getString(R.string.session_installer__require_user_interaction))
    }
}