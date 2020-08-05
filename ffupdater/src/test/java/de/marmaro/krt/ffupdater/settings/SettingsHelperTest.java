package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.utils.Utils;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
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
        sharedPreferences = mock(SharedPreferences.class);
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
        when(sharedPreferences.getBoolean("automaticCheck", true)).thenReturn(false);
        assertFalse(SettingsHelper.isAutomaticCheck(context));
    }

    @Test
    public void isAutomaticCheck_withValueTrue_returnTrue() {
        when(sharedPreferences.getBoolean("automaticCheck", true)).thenReturn(true);
        assertTrue(SettingsHelper.isAutomaticCheck(context));
    }

    @Test
    public void getCheckInterval_withNoValue_returnDefaultValue() {
        when(sharedPreferences.getString("checkInterval", null)).thenReturn(null);
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, SettingsHelper.getCheckInterval(context));
    }

    @Test
    public void getCheckInterval_withInvalidValue_returnDefaultValue() {
        when(sharedPreferences.getString("checkInterval", null)).thenReturn("exception");
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, SettingsHelper.getCheckInterval(context));
    }

    @Test
    public void getCheckInterval_withValue_returnDefaultValue() {
        when(sharedPreferences.getString("checkInterval", null)).thenReturn("42");
        assertEquals(42, SettingsHelper.getCheckInterval(context));
    }

    @Test
    public void getDisableApps_withNoApps_returnEmptySet() {
        Set<String> strings = new HashSet<>(Collections.emptyList());
        when(sharedPreferences.getStringSet("disabledApps", null)).thenReturn(strings);
        assertTrue(SettingsHelper.getDisableApps(context).isEmpty());
    }

    @Test
    public void getDisableApps_withSomeApps_returnApps() {
        Set<String> strings = new HashSet<>(Arrays.asList(
                "FIREFOX_KLAR",
                "FIREFOX_BETA",
                "LOCKWISE"
        ));
        when(sharedPreferences.getStringSet("disableApps", null)).thenReturn(strings);
        assertThat(SettingsHelper.getDisableApps(context), containsInAnyOrder(App.FIREFOX_KLAR, App.FIREFOX_BETA, App.LOCKWISE));
    }

    @Test
    public void getDisableApps_withInvalidApps_ignoreThem() {
        Set<String> strings = new HashSet<>(Collections.singletonList("FENIX"));
        when(sharedPreferences.getStringSet("disableApps", null)).thenReturn(strings);
        assertThat(SettingsHelper.getDisableApps(context), is(empty()));
    }

    @Test
    public void getDisableApps_withAllApps_returnApps() {
        Set<String> strings = new HashSet<>(Arrays.asList(
                "FIREFOX_KLAR",
                "FIREFOX_FOCUS",
                "FIREFOX_LITE",
                "FIREFOX_RELEASE",
                "FIREFOX_BETA",
                "FIREFOX_NIGHTLY",
                "LOCKWISE"
        ));
        when(sharedPreferences.getStringSet("disableApps", null)).thenReturn(strings);
        assertThat(SettingsHelper.getDisableApps(context), containsInAnyOrder(App.values()));
    }

    @Test
    public void getThemePreference_withNoValueAndBelowJellyBean_returnDefaultValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn(null);
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValueAndJellyBean_returnDefaultValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn(null);
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.JELLY_BEAN)).thenReturn(true);
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValueLollipop_returnDefaultValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn(null);
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.LOLLIPOP)).thenReturn(true);
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValueAndP_returnDefaultValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn(null);
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.P)).thenReturn(true);
        assertEquals(MODE_NIGHT_AUTO_BATTERY, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withNoValueAndQ_returnDefaultValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn(null);
        DeviceEnvironment environment = mock(DeviceEnvironment.class);
        when(environment.isSdkIntEqualOrHigher(Build.VERSION_CODES.Q)).thenReturn(true);
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, SettingsHelper.getThemePreference(context, environment));
    }

    @Test
    public void getThemePreference_withValue_returnValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn(String.valueOf(MODE_NIGHT_YES));
        assertEquals(MODE_NIGHT_YES, SettingsHelper.getThemePreference(context, new DeviceEnvironment()));
    }

    @Test
    public void getThemePreference_withIncorrectValue_returnDefaultValue() {
        when(sharedPreferences.getString("themePreference", null)).thenReturn("exception");
        assertEquals(MODE_NIGHT_NO, SettingsHelper.getThemePreference(context, new DeviceEnvironment()));
    }
}