package de.marmaro.krt.ffupdater;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.marmaro.krt.ffupdater.notification.Notificator;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.Utils;

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

            ListPreference themePreference = Objects.requireNonNull(findPreference("themePreference"));
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                AppCompatDelegate.setDefaultNightMode(Utils.stringToInt((String) newValue));
                return true;
            });
        }
    }
}