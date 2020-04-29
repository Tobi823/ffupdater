package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;

/**
 * Ask the user with this dialog if he really want to install the app.
 */
public class InstallationWarningAppDialog extends DialogFragment {
    static final String TAG = "warning_app_dialog";
    private final Consumer<App> downloadCallback;
    private final App app;

    InstallationWarningAppDialog(Consumer<App> callback, App app) {
        this.downloadCallback = callback;
        this.app = app;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.switch_to_unsafe_app_title))
                .setMessage(app.getWarning(requireContext()))
                .setPositiveButton(getString(R.string.switch_to_unsafe_app_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                    downloadCallback.accept(app);
                })
                .setNegativeButton(getString(R.string.switch_to_unsafe_app_negative_button), (dialog, which) -> dialog.dismiss())
                .create();
    }
}
