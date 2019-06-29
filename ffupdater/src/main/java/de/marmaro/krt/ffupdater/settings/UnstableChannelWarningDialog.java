package de.marmaro.krt.ffupdater.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import de.marmaro.krt.ffupdater.R;

/**
 * Created by Tobiwan on 29.06.2019.
 */
public class UnstableChannelWarningDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        String defaultBuildChannel = context.getString(R.string.default_pref_build);
        return new AlertDialog.Builder(getActivity())
                .setTitle(context.getString(R.string.switch_to_unsafe_build_title))
                .setMessage(context.getString(R.string.switch_to_unsafe_build_message))
                .setPositiveButton(context.getString(R.string.switch_to_unsafe_build_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(context.getString(R.string.switch_to_unsafe_build_negative_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(context)
                                .edit()
                                .putString(context.getString(R.string.pref_build), context.getString(R.string.default_pref_build))
                                .apply();
                        dialog.dismiss();
                        // force the reload of SettingsActivity
                        getActivity().finish();
                        startActivity(new Intent(context, SettingsActivity.class));
                    }
                })
                .create();
    }
}
