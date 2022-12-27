package de.marmaro.krt.ffupdater.installer

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.impl.IntentInstaller
import de.marmaro.krt.ffupdater.installer.impl.RootInstaller
import de.marmaro.krt.ffupdater.installer.impl.SessionInstaller
import de.marmaro.krt.ffupdater.installer.impl.ShizukuInstaller
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import java.io.File

interface AppInstaller : DefaultLifecycleObserver {
    suspend fun startInstallation(context: Context): InstallResult

    companion object {
        fun createForegroundAppInstaller(
            activity: ComponentActivity,
            app: App,
            file: File,
        ): AppInstaller {
            val registry = activity.activityResultRegistry
            return when (InstallerSettingsHelper(activity).getInstallerMethod()) {
                Installer.SESSION_INSTALLER -> SessionInstaller(app, file, true)
                Installer.NATIVE_INSTALLER -> IntentInstaller(activity, registry, app, file)
                Installer.ROOT_INSTALLER -> RootInstaller(app, file)
                Installer.SHIZUKU_INSTALLER -> ShizukuInstaller(app, file)
            }
        }

        fun createBackgroundAppInstaller(
            context: Context,
            app: App,
            file: File,
        ): AppInstaller {
            return when (InstallerSettingsHelper(context).getInstallerMethod()) {
                Installer.SESSION_INSTALLER -> SessionInstaller(app, file, false)
                Installer.NATIVE_INSTALLER -> throw Exception("Installer can not update apps in background")
                Installer.ROOT_INSTALLER -> RootInstaller(app, file)
                Installer.SHIZUKU_INSTALLER -> ShizukuInstaller(app, file)
            }
        }
    }
}