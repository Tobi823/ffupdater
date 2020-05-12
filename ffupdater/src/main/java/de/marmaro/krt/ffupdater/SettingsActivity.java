package de.marmaro.krt.ffupdater;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.notification.Notificator;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;

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
         * https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94
         */
        private void configureThemePreference() {
            ListPreference themePreference = Objects.requireNonNull(findPreference("themePreference"));
            Map<CharSequence, CharSequence> settings = new LinkedHashMap<>();
            settings.put("Light", Integer.toString(AppCompatDelegate.MODE_NIGHT_NO));
            settings.put("Dark", Integer.toString(AppCompatDelegate.MODE_NIGHT_YES));

            // Android Pie has a hidden dark theme setting and Android 10 an official dark theme setting
            if (Build.VERSION.SDK_INT >= 28) {
                settings.put("Use system default", Integer.toString(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
            }
            // Android Pie and below supports dark theme by Battery Saver
            if (Build.VERSION.SDK_INT < 29) {
                settings.put("Set by Battery Saver", Integer.toString(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY));
            }

            //
            if (Build.VERSION.SDK_INT > 28) {
                themePreference.setDefaultValue(Integer.toString(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
            } else {
                themePreference.setDefaultValue(Integer.toString(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY));
            }



            CharSequence[] entries = new CharSequence[elements];
            CharSequence[] values = new CharSequence[elements];


            themePreference.setEntries(new CharSequence[]{"aaa"});
            themePreference.setEntryValues(new CharSequence[]{"1"});
        }
    }
}