package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.installer.entity.Installer

class InstallerSettingsHelper {
    private val preferences: SharedPreferences

    constructor(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    fun getInstaller(): Installer {
        if (preferences.getBoolean("installer__root", false)) {
            return Installer.ROOT_INSTALLER
        }
        if (preferences.getBoolean("installer__native", false)) {
            return Installer.NATIVE_INSTALLER
        }
        if (preferences.getBoolean("installer__session", true)) {
            return Installer.SESSION_INSTALLER
        }
        return Installer.SESSION_INSTALLER
    }
}