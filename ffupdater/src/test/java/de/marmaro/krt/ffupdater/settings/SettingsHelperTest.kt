package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate.*
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.empty
import org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
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

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeTests() {
            mockkObject(DeviceEnvironment)
        }

        @JvmStatic
        @AfterClass
        fun afterTests() {
            unmockkObject(DeviceEnvironment)
        }
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
        sharedPreferences.edit().putStringSet(
            "disableApps",
            setOf(
                "BRAVE",
                "FIREFOX_BETA",
                "FIREFOX_FOCUS",
                "FIREFOX_KLAR",
                "FIREFOX_NIGHTLY",
                "FIREFOX_RELEASE",
                "ICERAVEN",
                "LOCKWISE",
                "BROMITE",
                "VIVALDI",
                "STYX"
            )
        ).commit()
        assertTrue(SettingsHelper(context).disabledApps.containsAll(App.values().toList()))
    }

    @Test
    fun getDisableApps_withRemovedApps_returnEmptyList() {
        sharedPreferences.edit().putStringSet(
            "disableApps", setOf(
                "FIREFOX_LITE",
                "FIREFOX_NIGHTLY"
            )
        ).commit()
        assertTrue(SettingsHelper(context).disabledApps.contains(App.FIREFOX_NIGHTLY))
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidPAndBelow_returnDefaultValue() {
        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidQAndHigher_returnDefaultValue() {
        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_null_returnDefault() {
        sharedPreferences.edit().putString("themePreference", null).commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_emptyString_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_text_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "lorem ipsum").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_nonExistingNumber_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "6").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_FOLLOW_SYSTEM_returnValue() {
        sharedPreferences.edit().putString("themePreference", "-1").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_NO_returnValue() {
        sharedPreferences.edit().putString("themePreference", "1").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_NO, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_NO, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_YES_returnValue() {
        sharedPreferences.edit().putString("themePreference", "2").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_YES, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_YES, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_AUTO_BATTERY_returnValue() {
        sharedPreferences.edit().putString("themePreference", "3").commit()

        every { DeviceEnvironment.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceEnvironment.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())
    }
}