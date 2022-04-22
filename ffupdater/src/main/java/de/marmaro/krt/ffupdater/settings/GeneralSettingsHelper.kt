package de.marmaro.krt.ffupdater.settings

import android.content.Context
import androidx.preference.PreferenceManager

class GeneralSettingsHelper(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val isRootUsageEnabled
        get() = preferences.getBoolean("general__use_root", false)
}