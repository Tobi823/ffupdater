package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadataRegister;

/**
 * Allow the user to select an app from the dialog to install.
 * Show warning or error message (if ABI is not supported) if necessary.
 */
public class InstallAppDialog extends DialogFragment {
    public static final String TAG = "download_new_app_dialog";

    private final Consumer<App> downloadCallback;
    private final DeviceEnvironment deviceEnvironment = new DeviceEnvironment();

    public InstallAppDialog(Consumer<App> callback) {
        this.downloadCallback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Context context = requireContext();
        final PackageManager packageManager = context.getPackageManager();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        final List<App> apps = new ArrayList<>(new InstalledMetadataRegister(packageManager, preferences).getNotInstalledApps());
        final CharSequence[] appNames = apps.stream().map(app -> app.getTitle(context)).toArray(CharSequence[]::new);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.install_application)
                .setItems(appNames, (dialog, which) -> {
                    App app = apps.get(which);
                    if (app.isIncompatibleWithDeviceAbi(deviceEnvironment)) {
                        new UnsupportedAbiDialog().show(getParentFragmentManager(), UnsupportedAbiDialog.TAG);
                    }
                    if (app.isIncompatibleWithDeviceApiLevel(deviceEnvironment)) {
                        new DeviceTooOldDialog(app, deviceEnvironment).show(getParentFragmentManager(), DeviceTooOldDialog.TAG);
                    }

                    if (app.isCompatibleWithDevice(deviceEnvironment)) {
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
