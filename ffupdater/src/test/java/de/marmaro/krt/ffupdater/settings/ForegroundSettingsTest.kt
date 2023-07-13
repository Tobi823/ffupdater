package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class ForegroundSettingsTest {

    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        ForegroundSettings.init(sharedPreferences)
        mockkObject(DeviceSdkTester)
    }

    companion object {
        @JvmStatic
        fun testDataForBooleanSettings(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "isUpdateCheckOnMeteredAllowed",
                "foreground__update_check__metered",
                true,
                { ForegroundSettings.isUpdateCheckOnMeteredAllowed }),

            Arguments.of(
                "isDownloadOnMeteredAllowed",
                "foreground__download__metered",
                true,
                { ForegroundSettings.isDownloadOnMeteredAllowed }),

            Arguments.of(
                "isDeleteUpdateIfInstallSuccessful",
                "foreground__delete_cache_if_install_successful",
                true,
                { ForegroundSettings.isDeleteUpdateIfInstallSuccessful }),

            Arguments.of(
                "isDeleteUpdateIfInstallFailed",
                "foreground__delete_cache_if_install_failed",
                true,
                { ForegroundSettings.isDeleteUpdateIfInstallFailed }),

            Arguments.of(
                "isHideWarningButtonForInstalledApps",
                "foreground__hide_warning_button_for_installed_apps",
                false,
                { ForegroundSettings.isHideWarningButtonForInstalledApps }),
        )
    }

    @ParameterizedTest(name = "has \"{0}\" the correct default value \"{2}\"")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct default value`(
        @Suppress("unused") name: String,
        @Suppress("unused") preferenceKey: String,
        defaultValue: Boolean,
        getValue: () -> Boolean,
    ) {
        val actual = getValue()
        assertEquals(defaultValue, actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changed to true")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changed to true`(
        @Suppress("unused") name: String,
        preferenceKey: String,
        @Suppress("unused") defaultValue: Boolean,
        getValue: () -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, true).commit()
        val actual = getValue()
        assertTrue(actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changed to false")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changed to false`(
        @Suppress("unused") name: String,
        preferenceKey: String,
        @Suppress("unused") defaultValue: Boolean,
        getValue: () -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, false).commit()
        val actual = getValue()
        assertFalse(actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changing values")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changing values`(
        @Suppress("unused") name: String,
        preferenceKey: String,
        @Suppress("unused") defaultValue: Boolean,
        getValue: () -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, false).commit()
        assertFalse(getValue())
        sharedPreferences.edit().putBoolean(preferenceKey, true).commit()
        assertTrue(getValue())
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidPAndBelow_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidQAndHigher_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_null_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", null).commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_emptyString_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_text_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "lorem ipsum").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_nonExistingNumber_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "6").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_FOLLOW_SYSTEM_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "-1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_NO_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_YES_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "2").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            ForegroundSettings.themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_AUTO_BATTERY_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "3").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettings.themePreference
        )
    }
}