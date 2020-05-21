package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.device.InstalledApps;

/**
 * Allow the user to select an app from the dialog to install.
 * Show warning or error message (if ABI is not supported) if necessary.
 */
public class InstallAppDialog extends DialogFragment {
    public static final String TAG = "download_new_app_dialog";

    private final Consumer<App> downloadCallback;
    private final DeviceEnvironment deviceABI = new DeviceEnvironment();

    public InstallAppDialog(Consumer<App> callback) {
        this.downloadCallback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        List<App> apps = InstalledApps.getNotInstalledApps(requireActivity().getPackageManager());
        CharSequence[] appNames = new CharSequence[apps.size()];
        for (int i = 0; i < appNames.length; i++) {
            appNames[i] = apps.get(i).getTitle(requireContext());
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.install_application)
                .setItems(appNames, (dialog, which) -> {
                    App app = apps.get(which);
                    if (app.isIncompatibleWithDeviceAbi(deviceABI)) {
                        new UnsupportedAbiDialog().show(getParentFragmentManager(), UnsupportedAbiDialog.TAG);
                    }
                    if (app.isIncompatibleWithDeviceApiLevel(deviceABI)) {
                        new DeviceTooOldDialog(app.getMinApiLevel()).show(getParentFragmentManager(), DeviceTooOldDialog.TAG);
                    }

                    if (app.isCompatibleWithDevice(deviceABI)) {
                        if (!app.getWarning(requireContext()).isEmpty()) {
                            new InstallationWarningAppDialog(downloadCallback, app).show(getParentFragmentManager(), InstallationWarningAppDialog.TAG);
                            return;
                        }
                        downloadCallback.accept(app);
                    }
                })
                .create();
    }
}
