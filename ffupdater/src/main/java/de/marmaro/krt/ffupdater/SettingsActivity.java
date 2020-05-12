package de.marmaro.krt.ffupdater;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.notification.Notificator;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.Utils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static de.marmaro.krt.ffupdater.settings.SettingsHelper.DEFAULT_THEME_PREFERENCE;
import static de.marmaro.krt.ffupdater.settings.SettingsHelper.getDisableApps;

/**
 * Activity for displaying the settings view.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        AppCompatDelegate.setDefaultNightMode(SettingsHelper.getThemePreference(this));
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Notificator.start(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            configureThemePreference();
        }

        /**
         * - Android Pie and below supports dark theme by Battery Saver
         * - Android Pie has a hidden dark theme setting and Android 10 an official dark theme setting
         * https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94
         */
        private void configureThemePreference() {
            ListPreference themePreference = Objects.requireNonNull(findPreference("themePreference"));
            Map<String, Integer> entries = new LinkedHashMap<>(SDK_INT == P ? 4 : 3);
            entries.put("Light", AppCompatDelegate.MODE_NIGHT_NO);
            entries.put("Dark", AppCompatDelegate.MODE_NIGHT_YES);

            if (SDK_INT >= P) {
                entries.put("Use system default", MODE_NIGHT_FOLLOW_SYSTEM);
            }
            if (SDK_INT <= P) {
                entries.put("Set by Battery Saver", MODE_NIGHT_AUTO_BATTERY);
            }

            themePreference.setEntries(Utils.stringsToCharSequenceArray(entries.keySet()));
            themePreference.setEntryValues(Utils.integersToCharSequenceArray(entries.values()));
            themePreference.setDefaultValue(DEFAULT_THEME_PREFERENCE);

            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                AppCompatDelegate.setDefaultNightMode(Utils.stringToInt((String) newValue, DEFAULT_THEME_PREFERENCE));
                return true;
            });
        }
    }
}