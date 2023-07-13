package de.marmaro.krt.ffupdater

import android.app.Application
import androidx.annotation.Keep
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import de.marmaro.krt.ffupdater.settings.NetworkSettings

@Keep
class FFUpdater : Application() {
    override fun onCreate() {
        super.onCreate()
        StrictModeSetup.enableStrictMode()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        BackgroundSettings.init(sharedPreferences)
        DataStoreHelper.init(sharedPreferences)
        DeviceSettingsHelper.init(sharedPreferences)
        ForegroundSettings.init(sharedPreferences)
        InstallerSettings.init(sharedPreferences)
        NetworkSettings.init(sharedPreferences)

        FileDownloader.init(applicationContext)

        Migrator.migrate(applicationContext)

        BackgroundJob.start(applicationContext.applicationContext, KEEP)

//        CrashListener.openCrashReporterForUncaughtExceptions(applicationContext)
    }

    companion object {
        const val LOG_TAG = "FFUpdater"
    }
}