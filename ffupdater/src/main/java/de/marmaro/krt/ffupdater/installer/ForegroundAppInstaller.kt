package de.marmaro.krt.ffupdater.installer

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.impl.ForegroundSessionInstaller
import de.marmaro.krt.ffupdater.installer.impl.IntentInstaller
import de.marmaro.krt.ffupdater.installer.impl.RootInstaller
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import java.io.File

interface ForegroundAppInstaller : AppInstaller, DefaultLifecycleObserver {
    companion object {
        fun create(activity: ComponentActivity, app: App, file: File): ForegroundAppInstaller {
            val registry = activity.activityResultRegistry
            return when (InstallerSettingsHelper(activity).getInstaller()) {
                Installer.SESSION_INSTALLER -> ForegroundSessionInstaller(activity, app, file)
                Installer.NATIVE_INSTALLER -> IntentInstaller(activity, registry, app, file)
                Installer.ROOT_INSTALLER -> RootInstaller(app, file)
            }
        }
    }
}