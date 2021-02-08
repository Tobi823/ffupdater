package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;

/**
 * Show a dialog with the app description.
 */
public class AppInfoDialog extends DialogFragment {
    private static final String TAG = "app_info_dialog";
    private final App app;

    public AppInfoDialog(App app) {
        this.app = app;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(app.getTitle(requireContext()))
                .setMessage(app.getDescription(requireContext()))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create();
    }

    public void show(@NonNull FragmentManager manager) {
        show(manager, TAG);
    }
}
