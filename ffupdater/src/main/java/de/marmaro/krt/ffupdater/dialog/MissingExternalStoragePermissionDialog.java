package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.marmaro.krt.ffupdater.R;

/**
 * Explain the user why the app needs the READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE permission.
 */
public class MissingExternalStoragePermissionDialog extends DialogFragment {
    public static final String TAG = "missing_external_storage_permission_dialog";
    private final Runnable downloadCallback;

    public MissingExternalStoragePermissionDialog(Runnable downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.missing_external_storage_permission_dialog_title)
                .setMessage(getString(R.string.missing_external_storage_permission_dialog_summary))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();
                    downloadCallback.run();
                })
                .create();
    }
}