package de.marmaro.krt.ffupdater.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.preference.DialogPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.background.UpdateChecker;

/**
 * Created by Tobiwan on 29.06.2019.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements DialogPreference.TargetFragment {
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
            UpdateChecker.registerOrUnregister(value);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener displayWarningOnSwitchingToUnsafeBuild = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            Context context = getContext();
            final ListPreference currentPreference = (ListPreference) preference;
            final String defaultBuildChannel = context.getString(R.string.default_pref_build);
            // abort when switching not from 'Release'
            if (!currentPreference.getValue().equals(defaultBuildChannel)) {
                return true;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(context.getString(R.string.switch_to_unsafe_build_title))
                    .setMessage(context.getString(R.string.switch_to_unsafe_build_message))
                    .setPositiveButton(context.getString(R.string.switch_to_unsafe_build_positive_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(context.getString(R.string.switch_to_unsafe_build_negative_button, currentPreference.getEntry()), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            currentPreference.setValue(defaultBuildChannel);
                            dialog.dismiss();
                        }
                    })
                    .show();
            return true;
        }
    };
}
