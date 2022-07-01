package de.marmaro.krt.ffupdater.installer

import android.content.Context
import de.marmaro.krt.ffupdater.app.MaintainedApp
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.impl.BackgroundSessionInstaller
import de.marmaro.krt.ffupdater.installer.impl.RootInstaller
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import java.io.File

interface BackgroundAppInstaller : AppInstaller {
    companion object {
        fun create(context: Context, app: MaintainedApp, file: File): BackgroundAppInstaller {
            return when (InstallerSettingsHelper(context).getInstaller()) {
                Installer.SESSION_INSTALLER -> BackgroundSessionInstaller(context, app, file)
                Installer.NATIVE_INSTALLER -> throw Exception("Installer can not update apps in background")
                Installer.ROOT_INSTALLER -> RootInstaller(app, file)
            }
        }
    }
}