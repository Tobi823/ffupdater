package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.marmaro.krt.ffupdater.R;

/**
 * Show the user that the app could not be installed because the operating system is too old.
 */
public class DeviceTooOldDialog extends DialogFragment {
    static final String TAG = "device_too_old_dialog";
    private final int necessaryApiLevel;
    private final String[] versionAndCodenames = new String[]{
            "1.0",
            "1.1",
            "1.5 Cupcake",
            "1.6 (Donut)",
            "2.0 (Eclair)",
            "2.0.1 (Eclair)",
            "2.1 (Eclair)",
            "2.2 (Froyo)",
            "2.3 (Gingerbread)",
            "2.3.3 (Gingerbread)",
            "3.0 (Honeycomb)",
            "3.1 (Honeycomb)",
            "3.2 (Honeycomb)",
            "4.0.1 (Ice Cream Sandwich)",
            "4.0.3 (Ice Cream Sandwich)",
            "4.1 (Jelly Bean)",
            "4.2 (Jelly Bean)",
            "4.3 (Jelly Bean)",
            "4.4 (KitKat)",
            "5.0 (Lollipop)",
            "5.1 (Lollipop)",
            "6.0 (Marshmallow)",
            "7.0 (Nougat)",
            "7.1 (Nougat)",
            "8.0.0 (Oreo)",
            "8.1.0 (Oreo)",
            "9 (Pie)",
            "10 (Android10)",
    };

    public DeviceTooOldDialog(int necessaryApiLevel) {
        this.necessaryApiLevel = necessaryApiLevel;
    }

    private String getVersionAndCodenameFromApiLevel(int apiLevel) {
        if (apiLevel >= versionAndCodenames.length) {
            return "*MISSING ENTRY IN CODE*";
        }
        return versionAndCodenames[apiLevel];
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.device_too_old_dialog_title)
                .setMessage(getString(R.string.device_too_old_dialog_message,
                        getVersionAndCodenameFromApiLevel(necessaryApiLevel),
                        getVersionAndCodenameFromApiLevel(Build.VERSION.SDK_INT)))
                .setNegativeButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create();
    }
}
