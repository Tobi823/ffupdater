package de.marmaro.krt.ffupdater.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.AvailableApps;
import de.marmaro.krt.ffupdater.R;

import static de.marmaro.krt.ffupdater.MainActivity.AVAILABLE_APPS_LOADER_ID;
import static de.marmaro.krt.ffupdater.MainActivity.TRIGGER_DOWNLOAD_FOR_APP;

/**
 * Created by Tobiwan on 04.10.2019.
 */
class WarningAppDialog extends DialogFragment {
    static final String TAG = "warning_app_dialog";
    private final LoaderManager.LoaderCallbacks<AvailableApps> callbacks;
    private final App app;

    WarningAppDialog(LoaderManager.LoaderCallbacks<AvailableApps> callbacks, App app) {
        this.callbacks = callbacks;
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
                    triggerDownload(app);
                })
                .setNegativeButton(getString(R.string.switch_to_unsafe_app_negative_button), (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void triggerDownload(App app) {
        FragmentActivity fragmentActivity = Objects.requireNonNull(getActivity());
        FragmentManager fragmentManager = Objects.requireNonNull(getFragmentManager());

        Bundle bundle = new Bundle();
        bundle.putString(TRIGGER_DOWNLOAD_FOR_APP, app.name());
        LoaderManager.getInstance(fragmentActivity).restartLoader(AVAILABLE_APPS_LOADER_ID, bundle, callbacks);
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
