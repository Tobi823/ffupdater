package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

object SettingsTestHelper {

    fun testBooleanSetting(
        settings: SharedPreferences,
        settingsName: String,
        default: Boolean,
        getValue: () -> Boolean,
    ) {
        assertEquals(default, getValue(), "Default value of setting $settingsName is incorrect")

        settings.edit().putBoolean(settingsName, false).apply()
        assertFalse(getValue(), "Settings value should be false")

        settings.edit().putBoolean(settingsName, true).apply()
        assertTrue(getValue(), "Settings value should be true")

        settings.edit().putBoolean(settingsName, false).apply()
        assertFalse(getValue(), "Settings value can't be changed (1/2)")
        settings.edit().putBoolean(settingsName, true).apply()
        assertTrue(getValue(), "Settings value can't be changed (2/2)")
    }
}