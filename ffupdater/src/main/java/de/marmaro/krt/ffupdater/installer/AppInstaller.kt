package de.marmaro.krt.ffupdater.installer

import android.content.Context
import kotlinx.coroutines.Deferred

interface AppInstaller {
    suspend fun installAsync(context: Context): Deferred<InstallResult>
}