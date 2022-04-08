package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate.*
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(MockKExtension::class)
class SettingsHelperTest {

    @MockK
    lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    companion object {
        @JvmStatic
        @BeforeAll
        internal fun beforeAll() {
            mockkObject(DeviceSdkTester)
        }

        @JvmStatic
        @AfterAll
        internal fun afterAll() {
            unmockkObject(DeviceSdkTester)
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
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.BRAVE))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxBeta_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_BETA")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.FIREFOX_BETA))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxFocus_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_FOCUS")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.FIREFOX_FOCUS))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxKlar_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_KLAR")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.FIREFOX_KLAR))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxNightly_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_NIGHTLY")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.FIREFOX_NIGHTLY))
    }

    @Test
    fun getDisableApps_withOneApp_FirefoxRelease_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("FIREFOX_RELEASE")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.FIREFOX_RELEASE))
    }

    @Test
    fun getDisableApps_withOneApp_Iceraven_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("ICERAVEN")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.ICERAVEN))
    }

    @Test
    fun getDisableApps_withOneApp_Lockwise_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("LOCKWISE")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.contains(App.LOCKWISE))
    }

    @Test
    fun getDisableApps_withInvalidApps_ignoreThem() {
        sharedPreferences.edit().putStringSet("disableApps", setOf("invalid")).commit()
        val disabledApps = SettingsHelper(context).disabledApps
        assertTrue(disabledApps.isEmpty())
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
                "UNGOOGLED_CHROMIUM",
                "FFUPDATER"
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
        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidQAndHigher_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_null_returnDefault() {
        sharedPreferences.edit().putString("themePreference", null).commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_emptyString_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_text_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "lorem ipsum").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withInvalidValue_nonExistingNumber_returnDefault() {
        sharedPreferences.edit().putString("themePreference", "6").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_FOLLOW_SYSTEM_returnValue() {
        sharedPreferences.edit().putString("themePreference", "-1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_NO_returnValue() {
        sharedPreferences.edit().putString("themePreference", "1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_NO, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_NO, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_YES_returnValue() {
        sharedPreferences.edit().putString("themePreference", "2").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_YES, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_YES, SettingsHelper(context).getThemePreference())
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_AUTO_BATTERY_returnValue() {
        sharedPreferences.edit().putString("themePreference", "3").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).getThemePreference())
    }
}