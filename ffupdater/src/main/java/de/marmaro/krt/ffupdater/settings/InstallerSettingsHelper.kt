package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import de.marmaro.krt.ffupdater.installer.entity.Installer

object InstallerSettingsHelper {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
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