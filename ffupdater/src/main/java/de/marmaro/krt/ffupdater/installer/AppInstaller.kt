package de.marmaro.krt.ffupdater.installer

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.impl.IntentInstaller
import de.marmaro.krt.ffupdater.installer.impl.RootInstaller
import de.marmaro.krt.ffupdater.installer.impl.SessionInstaller
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import kotlinx.coroutines.Deferred
import java.io.File

interface AppInstaller : DefaultLifecycleObserver {
    suspend fun installAsync(context: Context): Deferred<InstallResult>

    override fun onCreate(owner: LifecycleOwner) {
        throw Exception("Not needed by this installer")
    }

    companion object {
        fun createForegroundAppInstaller(activity: ComponentActivity, app: App, file: File): AppInstaller {
            val registry = activity.activityResultRegistry
            return when (InstallerSettingsHelper(activity).getInstaller()) {
                Installer.SESSION_INSTALLER -> SessionInstaller(activity, app, file, true)
                Installer.NATIVE_INSTALLER -> IntentInstaller(activity, registry, app, file)
                Installer.ROOT_INSTALLER -> RootInstaller(app, file)
            }
        }

        fun createBackgroundAppInstaller(context: Context, app: App, file: File): AppInstaller {
            return when (InstallerSettingsHelper(context).getInstaller()) {
                Installer.SESSION_INSTALLER -> SessionInstaller(context, app, file, false)
                Installer.NATIVE_INSTALLER -> throw Exception("Installer can not update apps in background")
                Installer.ROOT_INSTALLER -> RootInstaller(app, file)
            }
        }
    }
}