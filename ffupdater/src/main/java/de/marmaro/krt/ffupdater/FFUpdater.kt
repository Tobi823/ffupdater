package de.marmaro.krt.ffupdater

import android.app.Application
import androidx.annotation.Keep
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import de.marmaro.krt.ffupdater.BackgroundJob.Companion.start
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

@Keep
class FFUpdater : Application() {
    override fun onCreate() {
        super.onCreate()
        StrictModeSetup.enableStrictMode()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        BackgroundSettingsHelper.init(sharedPreferences)
        DataStoreHelper.init(sharedPreferences)
        DeviceSettingsHelper.init(sharedPreferences)
        ForegroundSettingsHelper.init(sharedPreferences)
        InstallerSettingsHelper.init(sharedPreferences)
        NetworkSettingsHelper.init(sharedPreferences)

        FileDownloader.init(applicationContext)

        Migrator().migrate(applicationContext)

        start(applicationContext.applicationContext, KEEP)
    }
}