package de.marmaro.krt.ffupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.utils.Utils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
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
        assertFalse(new SettingsHelper(context).isAutomaticCheck());
    }

    @Test
    public void isAutomaticCheck_withValueTrue_returnTrue() {
        sharedPreferences.edit().putBoolean("automaticCheck", true).commit();
        assertTrue(new SettingsHelper(context).isAutomaticCheck());
    }

    @Test
    public void getCheckInterval_withNoValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", null).commit();
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, new SettingsHelper(context).getCheckInterval());

        sharedPreferences.edit().putString("checkInterval", "").commit();
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, new SettingsHelper(context).getCheckInterval());
    }

    @Test
    public void getCheckInterval_withInvalidValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "invalid-value").commit();
        assertEquals(SettingsHelper.DEFAULT_CHECK_INTERVAL, new SettingsHelper(context).getCheckInterval());
    }

    @Test
    public void getCheckInterval_withValue_returnDefaultValue() {
        sharedPreferences.edit().putString("checkInterval", "42").commit();
        assertEquals(Duration.ofMinutes(42), new SettingsHelper(context).getCheckInterval());
    }

    @Test
    public void getDisableApps_withNoApps_returnEmptySet() {
        sharedPreferences.edit().putStringSet("disabledApps", null).commit();
        assertTrue(new SettingsHelper(context).getDisableApps().isEmpty());

        sharedPreferences.edit().putStringSet("disabledApps", new HashSet<>()).commit();
        assertTrue(new SettingsHelper(context).getDisableApps().isEmpty());
    }

    @Test
    public void getDisableApps_withSomeApps_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps",
                new HashSet<>(Arrays.asList("FIREFOX_KLAR", "FIREFOX_BETA", "LOCKWISE"))).commit();
        assertThat(new SettingsHelper(context).getDisableApps(), containsInAnyOrder(App.FIREFOX_KLAR, App.FIREFOX_BETA, App.LOCKWISE));
    }

    @Test
    public void getDisableApps_withInvalidApps_ignoreThem() {
        sharedPreferences.edit().putStringSet("disableApps", Utils.createSet("UNKNOWN_APP")).commit();
        assertThat(new SettingsHelper(context).getDisableApps(), is(empty()));
    }

    @Test
    public void getDisableApps_withAllApps_returnApps() {
        sharedPreferences.edit().putStringSet("disableApps", new HashSet<>(Arrays.asList(
                "FIREFOX_KLAR", "FIREFOX_FOCUS", "FIREFOX_LITE", "FIREFOX_RELEASE", "FIREFOX_BETA", "FIREFOX_NIGHTLY",
                "LOCKWISE", "BRAVE"
        ))).commit();
        assertThat(new SettingsHelper(context).getDisableApps(), containsInAnyOrder(App.values()));
    }

    @Test
    public void getThemePreference_withNoValue_BelowJellyBean_returnDefaultValue() {
        final DeviceEnvironment environment = mock(DeviceEnvironment.class);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_NO, new SettingsHelper(context).getThemePreference(environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_NO, new SettingsHelper(context).getThemePreference(environment));
    }

    @Test
    public void getThemePreference_withNoValue_JellyBean_returnDefaultValue() {
        final DeviceEnvironment environment = new DeviceEnvironment(Collections.singletonList(ARM), JELLY_BEAN);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_NO, new SettingsHelper(context).getThemePreference(environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_NO, new SettingsHelper(context).getThemePreference(environment));
    }

    @Test
    public void getThemePreference_withNoValue_Lollipop_returnDefaultValue() {
        final DeviceEnvironment environment = new DeviceEnvironment(Collections.singletonList(ARM), LOLLIPOP);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, new SettingsHelper(context).getThemePreference(environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, new SettingsHelper(context).getThemePreference(environment));
    }

    @Test
    public void getThemePreference_withNoValue_AndroidP_returnDefaultValue() {
        final DeviceEnvironment environment = new DeviceEnvironment(Collections.singletonList(ARM), P);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, new SettingsHelper(context).getThemePreference(environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_AUTO_BATTERY, new SettingsHelper(context).getThemePreference(environment));
    }

    @Test
    public void getThemePreference_withNoValue_AndroidQ_returnDefaultValue() {
        final DeviceEnvironment environment = new DeviceEnvironment(Collections.singletonList(ARM), Q);

        sharedPreferences.edit().putString("themePreference", null).commit();
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, new SettingsHelper(context).getThemePreference(environment));

        sharedPreferences.edit().putString("themePreference", "").commit();
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, new SettingsHelper(context).getThemePreference(environment));
    }

    @Test
    public void getThemePreference_withValue_returnValue() {
        final DeviceEnvironment deviceEnvironment = new DeviceEnvironment(Collections.singletonList(ARM), 30);
        sharedPreferences.edit().putString("themePreference", String.valueOf(MODE_NIGHT_YES)).commit();
        assertEquals(MODE_NIGHT_YES, new SettingsHelper(context).getThemePreference(deviceEnvironment));
    }

    @Test
    public void getThemePreference_withIncorrectValue_returnDefaultValueForApi30() {
        final DeviceEnvironment deviceEnvironment = new DeviceEnvironment(Collections.singletonList(ARM), 30);
        sharedPreferences.edit().putString("themePreference", "exception").commit();
        assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, new SettingsHelper(context).getThemePreference(deviceEnvironment));
    }
}