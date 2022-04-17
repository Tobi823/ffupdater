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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

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

        @JvmStatic
        fun excludedAppsFromBackgroundUpdateCheck_data(): Stream<Arguments> = Stream.of(
            Arguments.of(App.BRAVE, "BRAVE"),
            Arguments.of(App.BRAVE_BETA, "BRAVE_BETA"),
            Arguments.of(App.BRAVE_NIGHTLY, "BRAVE_NIGHTLY"),
            Arguments.of(App.BROMITE, "BROMITE"),
            Arguments.of(App.FFUPDATER, "FFUPDATER"),
            Arguments.of(App.FIREFOX_BETA, "FIREFOX_BETA"),
            Arguments.of(App.FIREFOX_FOCUS, "FIREFOX_FOCUS"),
            Arguments.of(App.FIREFOX_KLAR, "FIREFOX_KLAR"),
            Arguments.of(App.FIREFOX_NIGHTLY, "FIREFOX_NIGHTLY"),
            Arguments.of(App.FIREFOX_RELEASE, "FIREFOX_RELEASE"),
            Arguments.of(App.ICERAVEN, "ICERAVEN"),
            Arguments.of(App.LOCKWISE, "LOCKWISE"),
            Arguments.of(App.UNGOOGLED_CHROMIUM, "UNGOOGLED_CHROMIUM"),
            Arguments.of(App.VIVALDI, "VIVALDI"),
        )
    }

    @Test
    fun `isForegroundUpdateCheckOnMeteredAllowed test default value`() {
        assertTrue(SettingsHelper(context).isForegroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    fun `isForegroundUpdateCheckOnMeteredAllowed return true`() {
        sharedPreferences.edit().putBoolean("foreground__update_check__metered", true).commit()
        assertTrue(SettingsHelper(context).isForegroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    fun `isForegroundUpdateCheckOnMeteredAllowed return false`() {
        sharedPreferences.edit().putBoolean("foreground__update_check__metered", false).commit()
        assertFalse(SettingsHelper(context).isForegroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    fun `isForegroundUpdateCheckOnMeteredAllowed dont cache value`() {
        val settingsHelper = SettingsHelper(context)

        sharedPreferences.edit().putBoolean("foreground__update_check__metered", false).commit()
        assertFalse(settingsHelper.isForegroundUpdateCheckOnMeteredAllowed)

        sharedPreferences.edit().putBoolean("foreground__update_check__metered", true).commit()
        assertTrue(settingsHelper.isForegroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    fun `isForegroundDownloadOnMeteredAllowed test default value`() {
        assertTrue(SettingsHelper(context).isForegroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isForegroundDownloadOnMeteredAllowed return true`() {
        sharedPreferences.edit().putBoolean("foreground__download__metered", true).commit()
        assertTrue(SettingsHelper(context).isForegroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isForegroundDownloadOnMeteredAllowed return false`() {
        sharedPreferences.edit().putBoolean("foreground__download__metered", false).commit()
        assertFalse(SettingsHelper(context).isForegroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isForegroundDownloadOnMeteredAllowed dont cache value`() {
        val settingsHelper = SettingsHelper(context)

        sharedPreferences.edit().putBoolean("foreground__download__metered", false).commit()
        assertFalse(settingsHelper.isForegroundDownloadOnMeteredAllowed)

        sharedPreferences.edit().putBoolean("foreground__download__metered", true).commit()
        assertTrue(settingsHelper.isForegroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundUpdateCheckEnabled test default value`() {
        assertTrue(SettingsHelper(context).isBackgroundUpdateCheckEnabled)
    }

    @Test
    fun `isBackgroundUpdateCheckEnabled return true`() {
        sharedPreferences.edit().putBoolean("background__update_check__enabled", true).commit()
        assertTrue(SettingsHelper(context).isBackgroundUpdateCheckEnabled)
    }

    @Test
    fun `isBackgroundUpdateCheckEnabled return false`() {
        sharedPreferences.edit().putBoolean("background__update_check__enabled", false).commit()
        assertFalse(SettingsHelper(context).isBackgroundUpdateCheckEnabled)
    }

    @Test
    fun `isBackgroundUpdateCheckEnabled dont cache value`() {
        val settingsHelper = SettingsHelper(context)

        sharedPreferences.edit().putBoolean("background__update_check__enabled", false).commit()
        assertFalse(settingsHelper.isBackgroundUpdateCheckEnabled)

        sharedPreferences.edit().putBoolean("background__update_check__enabled", true).commit()
        assertTrue(settingsHelper.isBackgroundUpdateCheckEnabled)
    }

    @Test
    fun `backgroundUpdateCheckInterval with no settings`() {
        assertEquals(Duration.ofHours(6), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    fun `backgroundUpdateCheckInterval with invalid setting null`() {
        sharedPreferences.edit().putString("background__update_check__interval", null).commit()
        assertEquals(Duration.ofHours(6), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    fun `backgroundUpdateCheckInterval with invalid setting empty string`() {
        sharedPreferences.edit().putString("background__update_check__interval", "").commit()
        assertEquals(Duration.ofHours(6), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    fun `backgroundUpdateCheckInterval with invalid setting string`() {
        sharedPreferences.edit().putString("background__update_check__interval", "lorem ipsum").commit()
        assertEquals(Duration.ofHours(6), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    fun `backgroundUpdateCheckInterval with 42 minutes`() {
        sharedPreferences.edit().putString("background__update_check__interval", "42").commit()
        assertEquals(Duration.ofMinutes(42), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    fun `backgroundUpdateCheckInterval with too low value`() {
        sharedPreferences.edit().putString("background__update_check__interval", "-1").commit()
        assertEquals(Duration.ofMinutes(15), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    fun `backgroundUpdateCheckInterval with too high value`() {
        sharedPreferences.edit().putString("background__update_check__interval", "100000").commit()
        assertEquals(Duration.ofDays(28), SettingsHelper(context).backgroundUpdateCheckInterval)
    }

    @Test
    internal fun `isBackgroundUpdateCheckOnMeteredAllowed default value`() {
        assertTrue(SettingsHelper(context).isBackgroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    internal fun `isBackgroundUpdateCheckOnMeteredAllowed with false`() {
        sharedPreferences.edit().putBoolean("background__update_check__metered", false).commit()
        assertFalse(SettingsHelper(context).isBackgroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    internal fun `isBackgroundUpdateCheckOnMeteredAllowed with true`() {
        sharedPreferences.edit().putBoolean("background__update_check__metered", true).commit()
        assertTrue(SettingsHelper(context).isBackgroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundUpdateCheckOnMeteredAllowed with changing value`() {
        val settingsHelper = SettingsHelper(context)
        sharedPreferences.edit().putBoolean("background__update_check__metered", false).commit()
        assertFalse(settingsHelper.isBackgroundUpdateCheckOnMeteredAllowed)
        sharedPreferences.edit().putBoolean("background__update_check__metered", true).commit()
        assertTrue(settingsHelper.isBackgroundUpdateCheckOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundDownloadEnabled default value`() {
        assertTrue(SettingsHelper(context).isBackgroundDownloadEnabled)
    }

    @Test
    fun `isBackgroundDownloadEnabled with true`() {
        sharedPreferences.edit().putBoolean("background__download__enabled", true).commit()
        assertTrue(SettingsHelper(context).isBackgroundDownloadEnabled)
    }

    @Test
    fun `isBackgroundDownloadEnabled with false`() {
        sharedPreferences.edit().putBoolean("background__download__enabled", false).commit()
        assertFalse(SettingsHelper(context).isBackgroundDownloadEnabled)
    }

    @Test
    fun `isBackgroundDownloadEnabled with changing values`() {
        val settingsHelper = SettingsHelper(context)
        sharedPreferences.edit().putBoolean("background__download__enabled", false).commit()
        assertFalse(settingsHelper.isBackgroundDownloadEnabled)
        sharedPreferences.edit().putBoolean("background__download__enabled", true).commit()
        assertTrue(settingsHelper.isBackgroundDownloadEnabled)
    }

    @Test
    fun `isBackgroundDownloadOnMeteredAllowed default value`() {
        assertFalse(SettingsHelper(context).isBackgroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundDownloadOnMeteredAllowed with true`() {
        sharedPreferences.edit().putBoolean("background__download__metered", true).commit()
        assertTrue(SettingsHelper(context).isBackgroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundDownloadOnMeteredAllowed with false`() {
        sharedPreferences.edit().putBoolean("background__download__metered", false).commit()
        assertFalse(SettingsHelper(context).isBackgroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundDownloadOnMeteredAllowed with changing values`() {
        val settingsHelper = SettingsHelper(context)
        sharedPreferences.edit().putBoolean("background__download__metered", false).commit()
        assertFalse(settingsHelper.isBackgroundDownloadOnMeteredAllowed)
        sharedPreferences.edit().putBoolean("background__download__metered", true).commit()
        assertTrue(settingsHelper.isBackgroundDownloadOnMeteredAllowed)
    }

    @Test
    fun `isBackgroundInstallEnabled default value`() {
        assertFalse(SettingsHelper(context).isBackgroundInstallationEnabled)
    }

    @Test
    fun `isBackgroundInstallEnabled with true`() {
        sharedPreferences.edit().putBoolean("background__installation__enabled", true).commit()
        assertTrue(SettingsHelper(context).isBackgroundInstallationEnabled)
    }

    @Test
    fun `isBackgroundInstallEnabled with false`() {
        sharedPreferences.edit().putBoolean("background__installation__enabled", false).commit()
        assertFalse(SettingsHelper(context).isBackgroundInstallationEnabled)
    }

    @Test
    fun `isBackgroundInstallEnabled with changing values`() {
        val settingsHelper = SettingsHelper(context)
        sharedPreferences.edit().putBoolean("background__installation__enabled", false).commit()
        assertFalse(settingsHelper.isBackgroundInstallationEnabled)
        sharedPreferences.edit().putBoolean("background__installation__enabled", true).commit()
        assertTrue(settingsHelper.isBackgroundInstallationEnabled)
    }

    @Test
    fun `excludedAppsFromBackgroundUpdateCheck with default value`() {
        assertTrue(SettingsHelper(context).excludedAppsFromBackgroundUpdateCheck.isEmpty())
    }

    @Test
    fun `excludedAppsFromBackgroundUpdateCheck with null`() {
        sharedPreferences.edit().putStringSet("background__update_check__excluded_apps", null).commit()
        assertTrue(SettingsHelper(context).excludedAppsFromBackgroundUpdateCheck.isEmpty())
    }

    @Test
    fun `excludedAppsFromBackgroundUpdateCheck with empty set`() {
        sharedPreferences.edit().putStringSet("background__update_check__excluded_apps", setOf()).commit()
        assertTrue(SettingsHelper(context).excludedAppsFromBackgroundUpdateCheck.isEmpty())
    }

    @ParameterizedTest(name = "excludedAppsFromBackgroundUpdateCheck with app \"{0}\"")
    @MethodSource("excludedAppsFromBackgroundUpdateCheck_data")
    fun `excludedAppsFromBackgroundUpdateCheck with app X`(app: App, name: String) {
        sharedPreferences.edit()
            .putStringSet("background__update_check__excluded_apps", setOf(name))
            .commit()
        val disabledApps = SettingsHelper(context).excludedAppsFromBackgroundUpdateCheck
        assertTrue(app in disabledApps)
    }

    @Test
    fun `excludedAppsFromBackgroundUpdateCheck with invalid value`() {
        sharedPreferences.edit().putStringSet("background__update_check__excluded_apps", setOf("invalid"))
            .commit()
        val disabledApps = SettingsHelper(context).excludedAppsFromBackgroundUpdateCheck
        assertTrue(disabledApps.isEmpty())
    }

    @Test
    fun `excludedAppsFromBackgroundUpdateCheck with all apps`() {
        sharedPreferences.edit().putStringSet(
            "background__update_check__excluded_apps",
            setOf(
                "BRAVE",
                "BRAVE_BETA",
                "BRAVE_NIGHTLY",
                "BROMITE",
                "FFUPDATER",
                "FIREFOX_BETA",
                "FIREFOX_FOCUS",
                "FIREFOX_KLAR",
                "FIREFOX_NIGHTLY",
                "FIREFOX_RELEASE",
                "ICERAVEN",
                "LOCKWISE",
                "UNGOOGLED_CHROMIUM",
                "VIVALDI",
            )
        ).commit()
        assertEquals(
            App.values().toList().sorted(),
            SettingsHelper(context).excludedAppsFromBackgroundUpdateCheck.sorted()
        )
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidPAndBelow_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_userHasNotChangedSetting_AndroidQAndHigher_returnDefaultValue() {
        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withInvalidValue_null_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", null).commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withInvalidValue_emptyString_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withInvalidValue_text_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "lorem ipsum").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withInvalidValue_nonExistingNumber_returnDefault() {
        sharedPreferences.edit().putString("foreground__theme_preference", "6").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_FOLLOW_SYSTEM_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "-1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_NO_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "1").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_NO, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_NO, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_YES_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "2").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_YES, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_YES, SettingsHelper(context).themePreference)
    }

    @Test
    fun getThemePreference_withValidValue_MODE_NIGHT_AUTO_BATTERY_returnValue() {
        sharedPreferences.edit().putString("foreground__theme_preference", "3").commit()

        every { DeviceSdkTester.supportsAndroid10() } returns false
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)

        every { DeviceSdkTester.supportsAndroid10() } returns true
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper(context).themePreference)
    }
}