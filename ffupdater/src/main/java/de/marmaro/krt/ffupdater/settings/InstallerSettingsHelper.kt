package de.marmaro.krt.ffupdater.settings

import android.content.Context
import androidx.preference.PreferenceManager

class InstallerSettingsHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    enum class Installer {
        SESSION_INSTALLER,
        NATIVE_INSTALLER,
        ROOT_INSTALLER
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