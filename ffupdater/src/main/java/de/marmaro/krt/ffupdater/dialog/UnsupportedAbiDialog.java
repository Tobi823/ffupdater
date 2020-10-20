package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.marmaro.krt.ffupdater.R;

/**
 * Show the error that the app is not supported on this smartphone.
 */
public class UnsupportedAbiDialog extends DialogFragment {
    private static final String TAG = "unsupported_abi_dialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.unsupported_abi_dialog_title)
                .setMessage(R.string.unsupported_abi_dialog_message)
                .setNegativeButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .create();
    }

    public void show(@NonNull FragmentManager manager) {
        show(manager, TAG);
    }
}
