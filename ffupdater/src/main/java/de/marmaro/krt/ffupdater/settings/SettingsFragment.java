package de.marmaro.krt.ffupdater.settings;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.notification.NotificationCreator;

/**
 * Created by Tobiwan on 29.06.2019.
 */
class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        Preference prefCheckInterval = Objects.requireNonNull(findPreference(getString(R.string.pref_check_interval)));
        prefCheckInterval.setOnPreferenceChangeListener((preference, newValue) -> {
            NotificationCreator.register(getContext(), Integer.parseInt((String) newValue));
            return true;
        });
    }
}
