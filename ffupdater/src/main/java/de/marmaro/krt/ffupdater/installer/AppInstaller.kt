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
    val type: Installer
    suspend fun startInstallation(context: Context, file: File): InstallResult

    companion object {
        fun createForegroundAppInstaller(
            activity: ComponentActivity,
            app: App,
        ): AppInstaller {
            val registry = activity.activityResultRegistry
            return when (InstallerSettingsHelper(activity).getInstallerMethod()) {
                Installer.SESSION_INSTALLER -> SessionInstaller(app, true)
                Installer.NATIVE_INSTALLER -> IntentInstaller(activity, registry, app)
                Installer.ROOT_INSTALLER -> RootInstaller(app)
                Installer.SHIZUKU_INSTALLER -> ShizukuInstaller(app)
            }
        }

        fun createBackgroundAppInstaller(
            context: Context,
            app: App,
        ): AppInstaller {
            return when (InstallerSettingsHelper(context).getInstallerMethod()) {
                Installer.SESSION_INSTALLER -> SessionInstaller(app, false)
                Installer.NATIVE_INSTALLER -> throw Exception("Installer can not update apps in background")
                Installer.ROOT_INSTALLER -> RootInstaller(app)
                Installer.SHIZUKU_INSTALLER -> ShizukuInstaller(app)
            }
        }
    }
}