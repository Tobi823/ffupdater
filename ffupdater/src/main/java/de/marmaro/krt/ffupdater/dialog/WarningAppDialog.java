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
 * Created by Tobiwan on 04.10.2019.
 */
class WarningAppDialog extends DialogFragment {
    static final String TAG = "warning_app_dialog";
    private final Consumer<App> callback;
    private final App app;

    WarningAppDialog(Consumer<App> callback, App app) {
        this.callback = callback;
        this.app = app;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.switch_to_unsafe_app_title))
                .setMessage(getText())
                .setPositiveButton(getString(R.string.switch_to_unsafe_app_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                    downloadApp(app);
                })
                .setNegativeButton(getString(R.string.switch_to_unsafe_app_negative_button), (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void downloadApp(App app) {
        FragmentManager fragmentManager = Objects.requireNonNull(getFragmentManager());
        callback.accept(app);
        new FetchDownloadUrlDialog().show(fragmentManager, FetchDownloadUrlDialog.TAG);
    }

    private String getText() {
        switch (app) {
            case FENNEC_BETA:
            case FENNEC_NIGHTLY:
                return getString(R.string.switch_to_unsafe_fennec_message);
            case FENIX:
                return getString(R.string.switch_to_unsafe_fenix_message);
        }
        throw new IllegalArgumentException("unsupported app: " + app);
    }
}
