package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class ForegroundSettingsHelperTest {

    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
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

        @JvmStatic
        fun testDataForBooleanSettings(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "isUpdateCheckOnMeteredAllowed",
                "foreground__update_check__metered",
                true,
                { helper: ForegroundSettingsHelper -> helper.isUpdateCheckOnMeteredAllowed }),

            Arguments.of(
                "isDownloadOnMeteredAllowed",
                "foreground__download__metered",
                true,
                { helper: ForegroundSettingsHelper -> helper.isDownloadOnMeteredAllowed }),

            Arguments.of(
                "isDeleteUpdateIfInstallSuccessful",
                "foreground__delete_cache_if_install_successful",
                true,
                { helper: ForegroundSettingsHelper -> helper.isDeleteUpdateIfInstallSuccessful }),

            Arguments.of(
                "isDeleteUpdateIfInstallFailed",
                "foreground__delete_cache_if_install_failed",
                true,
                { helper: ForegroundSettingsHelper -> helper.isDeleteUpdateIfInstallFailed }),

            Arguments.of(
                "isHideWarningButtonForInstalledApps",
                "foreground__hide_warning_button_for_installed_apps",
                false,
                { helper: ForegroundSettingsHelper -> helper.isHideWarningButtonForInstalledApps }),
        )
    }

    @ParameterizedTest(name = "has \"{0}\" the correct default value \"{2}\"")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct default value`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (ForegroundSettingsHelper) -> Boolean,
    ) {
        val sut = ForegroundSettingsHelper(sharedPreferences)
        val actual = getValue(sut)
        Assertions.assertEquals(defaultValue, actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changed to true")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changed to true`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (ForegroundSettingsHelper) -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, true).commit()
        val sut = ForegroundSettingsHelper(sharedPreferences)
        val actual = getValue(sut)
        Assertions.assertTrue(actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changed to false")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changed to false`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (ForegroundSettingsHelper) -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, false).commit()
        val sut = ForegroundSettingsHelper(sharedPreferences)
        val actual = getValue(sut)
        Assertions.assertFalse(actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changing values")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changing values`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (ForegroundSettingsHelper) -> Boolean,
    ) {
        val sut = ForegroundSettingsHelper(sharedPreferences)
        sharedPreferences.edit().putBoolean(preferenceKey, false).commit()
        Assertions.assertFalse(getValue(sut))
        sharedPreferences.edit().putBoolean(preferenceKey, true).commit()
        Assertions.assertTrue(getValue(sut))
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidPAndBelow_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidQAndHigher_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_null_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", null).commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_emptyString_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_text_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "lorem ipsum").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withInvalidValue_nonExistingNumber_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "6").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_FOLLOW_SYSTEM_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "-1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_NO_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_YES_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "2").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_AUTO_BATTERY_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "3").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )

        every { DeviceSdkTester.supportsAndroid10() } returns true
        Assertions.assertEquals(
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            ForegroundSettingsHelper(sharedPreferences).themePreference
        )
    }
}