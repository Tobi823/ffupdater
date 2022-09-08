package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Bundle
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.BackgroundAppInstaller
import java.io.File

class BackgroundSessionInstaller(
    context: Context,
    app: App,
    file: File
) : BackgroundAppInstaller, AbstractSessionInstaller(context, app, file) {

    override val intentNameForAppInstallationCallback =
        "de.marmaro.krt.ffupdater.installer.impl.BackgroundSessionInstaller.app_installed"

    override fun requestInstallationPermission(context: Context, bundle: Bundle) {
        val status = bundle.getInt(PackageInstaller.EXTRA_STATUS)
        fail(
            "Installation failed because user interaction is required.",
            status,
            context.getString(R.string.session_installer__require_user_interaction)
        )
    }
}