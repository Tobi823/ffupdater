package de.marmaro.krt.ffupdater.device

import android.content.Context
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Keep
object StorageCleaner {
    suspend fun deleteApksOfNotInstalledApps(context: Context) {
        withContext(Dispatchers.IO) {
            Thread.sleep(10000)
            val installedApps = InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(context.applicationContext)
            App.values()
                .filter { it !in installedApps }
                .forEach { it.findImpl().deleteFileCache(context.applicationContext) }
        }
    }
}