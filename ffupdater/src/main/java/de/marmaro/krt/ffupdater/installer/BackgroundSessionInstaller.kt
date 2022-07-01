package de.marmaro.krt.ffupdater.installer

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Bundle
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.MaintainedApp
import java.io.File

class BackgroundSessionInstaller(
    context: Context,
    app: MaintainedApp,
    file: File
) : BackgroundAppInstaller, SessionInstallerBase(context, app, file) {

    override fun getIntentNameForAppInstallationCallback(): String {
        return "de.marmaro.krt.ffupdater.installer.BackgroundSessionInstaller.app_installed"
    }

    override fun requestInstallationPermission(context: Context, bundle: Bundle) {
        val status = bundle.getInt(PackageInstaller.EXTRA_STATUS)
        failure(status, context.getString(R.string.session_installer__require_user_interaction))
    }
}