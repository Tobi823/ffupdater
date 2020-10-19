package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.utils.Utils;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tobiwan on 03.05.2020.
 */
public class SettingsHelperTest {

    private Context context;
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        context = mock(Context.class);
        sharedPreferences = new SPMockBuilder().createSharedPreferences();
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
    }

    @Test
    public void stringToInt_withNull_returnFallback() {
        assertEquals(42, Utils.stringToInt(null, 42));
    }

    @Test
    public void stringToInt_withEmptyString_returnFallback() {
        assertEquals(24, Utils.stringToInt("", 24));
    }

    @Test
    public void stringToInt_withNumber_returnNumber() {
        assertEquals(30, Utils.stringToInt("30", 42));
    }

    @Test
    public void stringToInt_withNoNumber_returnFallback() {
        assertEquals(12, Utils.stringToInt("hi", 12));
    }

    @Test
    public void isAutomaticCheck_withValueFalse_returnFalse() {
        sharedPreferences.edit().putBoolean("automaticCheck", false).commit();
        assertFalse(SettingsHelper.isAutomaticCheck(context));
    }

    @Test
    public void isAutomaticCheck_withValueTrue_returnTrue() {
        sharedPreferences.edit().putBoolean("automaticCheck", true).commit();
        assertTrue(SettingsHelper.isAutomaticCheck(context));
    }

    @Test
    public void getCheckInterval_withNoValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", null).commit();
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, SettingsHelper.getCheckInterval(context));

        sharedPreferences.edit().putString("checkInterval", "").commit();
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, SettingsHelper.getCheckInterval(context));
    }

    @Test
    public void getCheckInterval_withInvalidValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "invalid-value").commit();
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, SettingsHelper.getCheckInterval(context));
    }

    @Test
    public void getCheckInterval_withValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "42").commit();
        assertEquals(42, SettingsHelper.getCheckInterval(context));
    }

    @Test
    public void getDisableApps_withNoApps_returnEmptySet() {
        sharedPreferences.edit().putStringSet("disabledApps", null).commit();
        assertTrue(SettingsHelper.getDisableApps(context).isEmpty());

        sharedPreferences.edit().putStringSet("disabledApps", new HashSet<>()).commit();
        assertTrue(SettingsHelper.getDisableApps(context).isEmpty());
    }

    @Test
    public void getDisableApps_withSomeApps_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps",
                new HashSet<>(Arrays.asList("FIREFOX_KLAR", "FIREFOX_BETA", "LOCKWISE"))).commit();
        assertThat(SettingsHelper.getDisableApps(context), containsInAnyOrder(App.FIREFOX_KLAR, App.FIREFOX_BETA, App.LOCKWISE));
    }

    @Test
    public void getDisableApps_withInvalidApps_ignoreThem() {
        sharedPreferences.edit().putStringSet("disableApps", Utils.createSet("UNKNOWN_APP")).commit();
        assertThat(SettingsHelper.getDisableApps(context), is(empty()));
    }

    @Test
    public void getDisableApps_withAllApps_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", new HashSet<>(Arrays.asList("FIREFOX_KLAR",
                "FIREFOX_FOCUS", "FIREFOX_LITE", "FIREFOX_RELEASE", "FIREFOX_BETA", "FIREFOX_NIGHTLY", "LOCKWISE"
        ))).commit();
        assertThat(SettingsHelper.getDisableApps(context), containsInAnyOrder(App.values()));
    }

    @Test
    public void getThemePreference_withNoValue_BelowJellyBean_returnDefaultValue() {
        final DeviceEnvironment environment = mock(DeviceEnvironment.class);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValue_JellyBean_returnDefaultValue() {
        final DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.JELLY_BEAN)).thenReturn(true);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValue_Lollipop_returnDefaultValue() {
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.LOLLIPOP)).thenReturn(true);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper.getThemePreference(context, environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValue_AndroidP_returnDefaultValue() {
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.P)).thenReturn(true);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper.getThemePreference(context, environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValue_AndroidQ_returnDefaultValue() {
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.Q)).thenReturn(true);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper.getThemePreference(context, environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withValue_returnValue() {
        sharedPreferences.edit().putString("themePreference", String.valueOf(MODE_NIGHT_YES)).commit();
        assertEquals(MODE_NIGHT_YES, SettingsHelper.getThemePreference(context, new DeviceEnvironment()));
    }

    @Test
    public void getThemePreference_withIncorrectValue_returnDefaultValue() {
        sharedPreferences.edit().putString("themePreference", "exception").commit();
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, new DeviceEnvironment()));
    }
}