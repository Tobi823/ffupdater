package de.marmaro.krt.ffupdater.installer

import android.content.Context
import androidx.annotation.Keep
import androidx.lifecycle.DefaultLifecycleObserver
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import de.marmaro.krt.ffupdater.installer.entity.Installer
import java.io.File

@Keep
interface AppInstaller : DefaultLifecycleObserver {

    suspend fun startInstallation(context: Context, file: File, appImpl: AppBase): InstallResult
}