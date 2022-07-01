package de.marmaro.krt.ffupdater.installer

import android.content.Context
import de.marmaro.krt.ffupdater.installer.entity.InstallResult
import kotlinx.coroutines.Deferred

interface AppInstaller {
    suspend fun installAsync(context: Context): Deferred<InstallResult>
}