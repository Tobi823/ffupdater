package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.marmaro.krt.ffupdater.R;

/**
 * Created by Tobiwan on 21.04.2020.
 */
public class UnsupportedAbiDialog extends DialogFragment {
    static final String TAG = "unsupported_abi_dialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.unsupported_abi_dialog_title)
                .setMessage(R.string.unsupported_abi_dialog_message)
                .setNegativeButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .create();
    }
}
