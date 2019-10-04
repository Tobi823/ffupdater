package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.AvailableApps;
import de.marmaro.krt.ffupdater.R;

import static de.marmaro.krt.ffupdater.MainActivity.AVAILABLE_APPS_LOADER_ID;
import static de.marmaro.krt.ffupdater.MainActivity.TRIGGER_DOWNLOAD_FOR_APP;

/**
 * Created by Tobiwan on 04.10.2019.
 */
public class WarningAppDialog extends DialogFragment {
    public static final String TAG = "warning_app_dialog";
    private LoaderManager.LoaderCallbacks<AvailableApps> callbacks;
    private App app;

    public WarningAppDialog(LoaderManager.LoaderCallbacks<AvailableApps> callbacks, App app) {
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
        Bundle bundle = new Bundle();
        bundle.putString(TRIGGER_DOWNLOAD_FOR_APP, app.name());
        LoaderManager.getInstance(getActivity()).restartLoader(AVAILABLE_APPS_LOADER_ID, bundle, callbacks);
        new FetchDownloadUrlDialog().show(getFragmentManager(), FetchDownloadUrlDialog.TAG);
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
