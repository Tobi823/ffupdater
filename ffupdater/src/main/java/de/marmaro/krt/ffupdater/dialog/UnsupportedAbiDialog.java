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
                .setTitle("Unsupported device")
                .setMessage("The selected browser is incompatible with your smartphone's processor. Please select a different browser.")
                .setNegativeButton("Ok", (dialog, which) -> dialog.dismiss())
                .create();
    }
}
