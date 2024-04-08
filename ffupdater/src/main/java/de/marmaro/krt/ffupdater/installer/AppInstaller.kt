package de.marmaro.krt.ffupdater.installer

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import androidx.lifecycle.DefaultLifecycleObserver
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.impl.IntentInstaller
import de.marmaro.krt.ffupdater.installer.impl.RootInstaller
import de.marmaro.krt.ffupdater.installer.impl.SessionInstaller
import de.marmaro.krt.ffupdater.installer.impl.ShizukuInstaller
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import java.io.File

@Keep
interface AppInstaller : DefaultLifecycleObserver {
    val type: Installer

    fun changeApp(app: App)

    suspend fun startInstallation(context: Context, file: File): InstallResult
}