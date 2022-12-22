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

    fun getInstallerMethod(): Installer {
        val methodName = preferences.getString("installer__method", Installer.SESSION_INSTALLER.name)
            ?: return Installer.SESSION_INSTALLER
        return try {
            Installer.valueOf(methodName)
        } catch (e: IllegalArgumentException) {
            Installer.SESSION_INSTALLER
        }
    }
}