package de.marmaro.krt.ffupdater

import android.app.Application
import androidx.annotation.Keep
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.WorkManager
import de.marmaro.krt.ffupdater.background.BackgroundWork
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.device.PowerSaveModeReceiver
import de.marmaro.krt.ffupdater.device.PowerUtil
import de.marmaro.krt.ffupdater.device.StorageCleaner
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.settings.NetworkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Keep
class FFUpdater : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashListener.showNotificationForUncaughtException(applicationContext)

        StrictModeSetup.enableStrictMode()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        BackgroundSettings.init(sharedPreferences)
        DataStoreHelper.init(sharedPreferences)
        DeviceSettingsHelper.init(sharedPreferences)
        ForegroundSettings.init(sharedPreferences)
        InstallerSettings.init(sharedPreferences)
        NetworkSettings.init(sharedPreferences)

        PowerUtil.init(applicationContext)
        FileDownloader.init()
        Migrator.migrate(applicationContext)

        PowerSaveModeReceiver.register(applicationContext, sharedPreferences)

        cleanupUnusedApkFiles()

        initializeWorkManagerIfAndroidForgotIt()
        startWorkManager()
    }

    private fun cleanupUnusedApkFiles() {
        CoroutineScope(Job() + Dispatchers.Main).launch {
            StorageCleaner.deleteApksOfNotInstalledApps(applicationContext)
        }
    }

    private fun initializeWorkManagerIfAndroidForgotIt() {
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(applicationContext,
                Configuration.Builder()
                    .build()
            )
        }
    }

    private fun startWorkManager() {
        CoroutineScope(Job() + Dispatchers.Main).launch {
            InstalledAppsCache.updateCache(applicationContext)
            if (Migrator.wasAppUpdated()) {
                BackgroundWork.forceRestart(applicationContext)
            } else {
                BackgroundWork.start(applicationContext)
            }
        }
    }

    companion object {
        const val LOG_TAG = "FFUpdater"
    }
}