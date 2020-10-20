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
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.utils.Utils;

/**
 * Show the user that the app could not be installed because the operating system is too old.
 */
public class DeviceTooOldDialog extends DialogFragment {
    private static final String TAG = "device_too_old_dialog";
    private final App app;
    private final DeviceEnvironment deviceEnvironment;

    public DeviceTooOldDialog(App app, DeviceEnvironment deviceEnvironment) {
        this.app = app;
        this.deviceEnvironment = deviceEnvironment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.device_too_old_dialog_title)
                .setMessage(getString(R.string.device_too_old_dialog_message,
                        Utils.getVersionAndCodenameFromApiLevel(app.getMinApiLevel()),
                        Utils.getVersionAndCodenameFromApiLevel(deviceEnvironment.getApiLevel())))
                .setNegativeButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create();
    }

    public void show(@NonNull FragmentManager manager) {
        show(manager, TAG);
    }
}
