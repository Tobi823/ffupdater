package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;

import java.util.List;
import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.device.DeviceABI;
import de.marmaro.krt.ffupdater.device.InstalledApps;

import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.ARM;

/**
 * Created by Tobiwan on 23.08.2019.
 */
public class DownloadNewAppDialog extends DialogFragment {
    private final Consumer<App> callback;

    public DownloadNewAppDialog(Consumer<App> callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Pair<List<App>, CharSequence[]> notInstalledApps = getNotInstalledApps();
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.download_new_app)
                .setItems(notInstalledApps.second, (dialog, which) -> {
                    App app = Objects.requireNonNull(notInstalledApps.first).get(which);
                    switch (app) {
                        case FENNEC_BETA:
                        case FENNEC_NIGHTLY:
                            new WarningAppDialog(callback, app).show(getParentFragmentManager(), WarningAppDialog.TAG);
                            break;
                        case FIREFOX_KLAR:
                        case FIREFOX_FOCUS:
                            if (DeviceABI.getAbi().equals(AARCH64) || DeviceABI.getAbi().equals(ARM)) {
                                callback.accept(app);
                            } else {
                                new UnsupportedAbiDialog().show(getParentFragmentManager(), UnsupportedAbiDialog.TAG);
                            }
                            break;
                        default:
                            callback.accept(app);
                    }
                })
                .create();
    }

    @NonNull
    private Pair<List<App>, CharSequence[]> getNotInstalledApps() {
        InstalledApps detector = new InstalledApps(requireActivity().getPackageManager());
        List<App> notInstalledApps = detector.getNotInstalledApps();

        int appsCount = notInstalledApps.size();
        CharSequence[] appNames = new CharSequence[appsCount];
        for (int i = 0; i < appsCount; i++) {
            appNames[i] = notInstalledApps.get(i).getTitle(requireActivity());
        }
        return new Pair<>(notInstalledApps, appNames);
    }
}
