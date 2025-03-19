package de.marmaro.krt.ffupdater.device

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList

@Keep
object InstalledAppsCache {
    private var installedCorrectFingerprint = mutableListOf<App>()
    private var installedDifferentFingerprint = mutableListOf<App>()
    private var isInitialized = false
    private val mutex = Mutex()
    private var lastUpdate = 0L

    suspend fun getAppsApplicableForBackgroundUpdate(context: Context): List<App> {
        return getInstalledAppsWithCorrectSignature(context).asSequence()
            .filter { it !in BackgroundSettings.excludedAppsFromUpdateCheck }
            .filter { it !in ForegroundSettings.hiddenApps }
            .filter { DeviceAbiExtractor.supportsOneOf(it.findImpl().supportedAbis) }.map { it.findImpl() }
            .filter { !it.wasInstalledByOtherApp(context) }.map { it.app }.toList()
    }

    suspend fun getInstalledAppsWithCorrectSignature(context: Context): List<App> {
        initializeCacheIfNecessary(context.applicationContext)
        return installedCorrectFingerprint.toImmutableList()
    }

    suspend fun getInstalledAppsWithDifferentSignature(context: Context): List<App> {
        initializeCacheIfNecessary(context.applicationContext)
        return installedDifferentFingerprint.toImmutableList()
    }

    private suspend fun initializeCacheIfNecessary(context: Context) {
        if (!isInitialized) {
            updateCache(context)
            isInitialized = true
        }
    }

    suspend fun updateCache(context: Context) {
        withContext(Dispatchers.Default) {
            if (!wasCacheUpdatedRecently()) {
                mutex.withLock {
                    if (!wasCacheUpdatedRecently()) {
                        updateCacheHelper(context)
                    }
                }
            }
        }
    }

    private suspend fun updateCacheHelper(context: Context) {
        Log.i(LOG_TAG, "InstalledAppsCache: Update cache of installed apps.")
        val correctApps = mutableListOf<App>()
        val differentApps = mutableListOf<App>()
        App.values().forEach {
            when (it.findImpl().isInstalled(context.applicationContext)) {
                InstallationStatus.INSTALLED -> correctApps.add(it)
                InstallationStatus.INSTALLED_WITH_DIFFERENT_FINGERPRINT -> differentApps.add(it)
                else -> {}
            }
        }
        installedCorrectFingerprint = correctApps
        installedDifferentFingerprint = differentApps
        lastUpdate = System.currentTimeMillis()
        Log.i(LOG_TAG, "InstalledAppsCache: Cache was updated.")
    }

    private fun wasCacheUpdatedRecently(): Boolean {
        return (System.currentTimeMillis() - lastUpdate) <= 1000
    }
}
