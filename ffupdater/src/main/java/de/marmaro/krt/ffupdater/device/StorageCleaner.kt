package de.marmaro.krt.ffupdater.device

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Keep
object StorageCleaner {
    suspend fun deleteApksOfNotInstalledApps(context: Context) {
        withContext(Dispatchers.IO) {
            val installedApps = InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(context.applicationContext)
            App.values()
                .filter { it !in installedApps }
                .forEach {
                    Log.i(LOG_TAG, "StorageCleaner: Delete possible cached files of $it")
                    it.findImpl().deleteFileCache(context.applicationContext)
                }
        }
    }
}