package de.marmaro.krt.ffupdater.settings;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.NotificationCreator;

/**
 * Created by Tobiwan on 29.06.2019.
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        Preference prefCheckInterval = findPreference(getString(R.string.pref_check_interval));
        prefCheckInterval.setOnPreferenceChangeListener(reconfigureUpdateCheckerOnChange);

        Preference prefBuild = findPreference(getString(R.string.pref_build));
        prefBuild.setOnPreferenceChangeListener(displayWarningOnSwitchingToUnsafeBuild);
    }

    private Preference.OnPreferenceChangeListener reconfigureUpdateCheckerOnChange = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String valueAsString = (String) newValue;
            int value = Integer.parseInt(valueAsString);
            NotificationCreator.registerOrUnregister(value);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener displayWarningOnSwitchingToUnsafeBuild = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            final ListPreference currentPreference = (ListPreference) preference;
            String defaultBuildChannel = getContext().getString(R.string.default_pref_build);

            if (defaultBuildChannel.equals(currentPreference.getValue()) && !defaultBuildChannel.equals(newValue)) {
                new UnstableChannelWarningDialog().show(getFragmentManager(), "unstable_channel_warning_dialog");
            }
            return true;
        }
    };
}
