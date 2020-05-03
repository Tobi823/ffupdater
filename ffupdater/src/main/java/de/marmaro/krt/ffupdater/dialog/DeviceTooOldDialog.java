package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.utils.Utils;

/**
 * Show the user that the app could not be installed because the operating system is too old.
 */
public class DeviceTooOldDialog extends DialogFragment {
    static final String TAG = "device_too_old_dialog";
    private final int necessaryApiLevel;

    public DeviceTooOldDialog(int necessaryApiLevel) {
        this.necessaryApiLevel = necessaryApiLevel;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.device_too_old_dialog_title)
                .setMessage(getString(R.string.device_too_old_dialog_message,
                        Utils.getVersionAndCodenameFromApiLevel(necessaryApiLevel),
                        Utils.getVersionAndCodenameFromApiLevel(Build.VERSION.SDK_INT)))
                .setNegativeButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create();
    }
}
