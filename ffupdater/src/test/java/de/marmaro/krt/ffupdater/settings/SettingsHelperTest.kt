package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.*
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.empty
import org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Duration


class SettingsHelperTest {

    @MockK
    lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    @Test
    fun getAutomaticCheck_userHasNotChangedSetting_returnDefaultValueTrue() {
        assertTrue(SettingsHelper(context).automaticCheck)
    }

    @Test
    fun getAutomaticCheck_userEnabledIt_returnTrue() {
        sharedPreferences.edit().putBoolean("automaticCheck", true).commit()
        assertTrue(SettingsHelper(context).automaticCheck)
    }

    @Test
    fun getAutomaticCheck_userDisabledIt_returnFalse() {
        sharedPreferences.edit().putBoolean("automaticCheck", false).commit()
        assertFalse(SettingsHelper(context).automaticCheck)
    }

    private val defaultCheckIntervalDuration = Duration.ofHours(6)

    @Test
    fun getCheckInterval_userHasNotChangedSetting_returnDefaultValue() {
        assertEquals(defaultCheckIntervalDuration, SettingsHelper(context).checkInterval)
    }

    @Test
    fun getCheckInterval_withInvalidStoredData_null_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", null).commit()
        assertEquals(defaultCheckIntervalDuration, SettingsHelper(context).checkInterval)
    }

    @Test
    fun getCheckInterval_withInvalidStoredData_emptyString_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "").commit()
        assertEquals(defaultCheckIntervalDuration, SettingsHelper(context).checkInterval)
    }

    @Test
    fun getCheckInterval_withInvalidStoredData_text_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "lorem ipsum").commit()
        assertEquals(defaultCheckIntervalDuration, SettingsHelper(context).checkInterval)
    }

    @Test
    fun getCheckInterval_withPositiveValue_returnValue() {
        sharedPreferences.edit().putString("checkInterval", "42").commit()
        assertEquals(Duration.ofMinutes(42), SettingsHelper(context).checkInterval)
    }

    @Test
    fun getCheckInterval_withNegativeValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "-1").commit()
        assertEquals(defaultCheckIntervalDuration, SettingsHelper(context).checkInterval)
    }

    @Test
    fun getCheckInterval_withTooHighNumber_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "57600" /*40 days*/).commit()
        assertEquals(defaultCheckIntervalDuration, SettingsHelper(context).checkInterval)
    }

    @Test
    fun getDisableApps_userHasNotChangedSetting_returnEmptySet() {
        assertTrue(SettingsHelper(context).disabledApps.isEmpty())
    }

    @Test
    fun getDisableApps_withValue_null_returnEmptySet() {
        sharedPreferences.edit().putStringSet("disabledApps", null).commit()
        assertTrue(SettingsHelper(context).disabledApps.isEmpty())
    }

    @Test
    fun getDisableApps_withEmptySet_returnEmptySet() {
        sharedPreferences.edit().putStringSet("disabledApps", setOf()).commit()
        assertTrue(SettingsHelper(context).disabledApps.isEmpty())
    }

    @Test
    fun getDisableApps_withOneApp_Brave_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("BRAVE")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.BRAVE))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxBeta_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_BETA")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.FIREFOX_BETA))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxFocus_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_FOCUS")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.FIREFOX_FOCUS))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxKlar_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_KLAR")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.FIREFOX_KLAR))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxLite_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_LITE")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.FIREFOX_LITE))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxNightly_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_NIGHTLY")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.FIREFOX_NIGHTLY))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxRelease_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_RELEASE")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.FIREFOX_RELEASE))
    }

    @Test
    fun getDisableApps_withOneApp_Iceraven_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("ICERAVEN")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.ICERAVEN))
    }

    @Test
    fun getDisableApps_withOneApp_Lockwise_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("LOCKWISE")).commit()
        assertThat(SettingsHelper(context).disabledApps, containsInAnyOrder(App.LOCKWISE))
    }

    @Test
    fun getDisableApps_withInvalidApps_ignoreThem() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("invalid")).commit()
        assertThat(SettingsHelper(context).disabledApps, `is`(empty()))
    }

    @Test
    fun getDisableApps_withAllApps_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("BRAVE", "FIREFOX_BETA",
                "FIREFOX_FOCUS", "FIREFOX_KLAR", "FIREFOX_LITE", "FIREFOX_NIGHTLY",
                "FIREFOX_RELEASE", "ICERAVEN", "LOCKWISE")).commit()
        assertTrue(SettingsHelper(context).disabledApps.containsAll(App.values().toList()))
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidPAndBelow_returnDefaultValue() {
        for (sdkInt in Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.P) {
            val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), sdkInt)
            val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
            assertEquals(MODE_NIGHT_AUTO_BATTERY, actual)
        }
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidQAndHigher_returnDefaultValue() {
        for (sdkInt in Build.VERSION_CODES.Q..Build.VERSION_CODES.R) {
            val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), sdkInt)
            val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
            assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, actual)
        }
    }

    private fun testIfGetThemePreferenceReturnsDefaultValue() {
        for (sdkInt in Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.P) {
            val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), sdkInt)
            val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
            assertEquals(MODE_NIGHT_AUTO_BATTERY, actual)
        }
        for (sdkInt in Build.VERSION_CODES.Q..Build.VERSION_CODES.R) {
            val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), sdkInt)
            val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
            assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, actual)
        }
    }

    @Test
    fun getThemePreference_withInvalidValue_null_returnDefault() {
        sharedPreferences.edit().putString("themePreference", null).commit()
        testIfGetThemePreferenceReturnsDefaultValue()
    }

    @Test
    fun getThemePreference_withInvalidValue_emptyString_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "").commit()
        testIfGetThemePreferenceReturnsDefaultValue()
    }

    @Test
    fun getThemePreference_withInvalidValue_text_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "lorem ipsum").commit()
        testIfGetThemePreferenceReturnsDefaultValue()
    }

    @Test
    fun getThemePreference_withInvalidValue_nonExistingNumber_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "6").commit()
        testIfGetThemePreferenceReturnsDefaultValue()
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_FOLLOW_SYSTEM_returnValue() {
        sharedPreferences.edit().putString("themePreference", "-1").commit()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)
        val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, actual)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_NO_returnValue() {
        sharedPreferences.edit().putString("themePreference", "1").commit()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)
        val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
        assertEquals(MODE_NIGHT_NO, actual)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_YES_returnValue() {
        sharedPreferences.edit().putString("themePreference", "2").commit()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)
        val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
        assertEquals(MODE_NIGHT_YES, actual)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_AUTO_BATTERY_returnValue() {
        sharedPreferences.edit().putString("themePreference", "3").commit()
        val deviceEnvironment = DeviceEnvironment(listOf(ABI.ARMEABI_V7A), Build.VERSION_CODES.R)
        val actual = SettingsHelper(context).getThemePreference(deviceEnvironment)
        assertEquals(MODE_NIGHT_AUTO_BATTERY, actual)
    }
}