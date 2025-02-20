package de.marmaro.krt.ffupdater.installer

import androidx.activity.ComponentActivity
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.impl.IntentInstaller
import de.marmaro.krt.ffupdater.installer.impl.RootInstaller
import de.marmaro.krt.ffupdater.installer.impl.SessionInstaller
import de.marmaro.krt.ffupdater.installer.impl.ShizukuInstaller
import de.marmaro.krt.ffupdater.settings.InstallerSettings

object AppInstallerFactory {
    fun createForegroundAppInstaller(
        activity: ComponentActivity,
    ): AppInstaller {
        val registry = activity.activityResultRegistry
        return when (InstallerSettings.getInstallerMethod()) {
            Installer.SESSION_INSTALLER -> SessionInstaller(true)
            Installer.NATIVE_INSTALLER -> IntentInstaller(activity.applicationContext, registry)
            Installer.ROOT_INSTALLER -> RootInstaller()
            Installer.SHIZUKU_INSTALLER -> ShizukuInstaller()
        }
    }

    fun createBackgroundAppInstaller(): AppInstaller {
        return when (InstallerSettings.getInstallerMethod()) {
            Installer.SESSION_INSTALLER -> SessionInstaller(false)
            Installer.NATIVE_INSTALLER -> throw Exception("Installer can not update apps in background")
            Installer.ROOT_INSTALLER -> RootInstaller()
            Installer.SHIZUKU_INSTALLER -> ShizukuInstaller()
        }
    }
}