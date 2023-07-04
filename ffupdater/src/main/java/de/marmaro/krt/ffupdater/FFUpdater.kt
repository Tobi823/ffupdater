package de.marmaro.krt.ffupdater

import android.app.Application
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.InstallerSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

class FFUpdater : Application() {
    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        BackgroundSettingsHelper.init(sharedPreferences)
        DataStoreHelper.init(sharedPreferences)
        DeviceSettingsHelper.init(sharedPreferences)
        ForegroundSettingsHelper.init(sharedPreferences)
        InstallerSettingsHelper.init(sharedPreferences)
        NetworkSettingsHelper.init(sharedPreferences)

        FileDownloader.init(applicationContext)

        Migrator().migrate(applicationContext)
    }
}