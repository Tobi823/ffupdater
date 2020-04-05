package de.marmaro.krt.ffupdater.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import java.util.List;
import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.AvailableApps;
import de.marmaro.krt.ffupdater.InstalledApps;
import de.marmaro.krt.ffupdater.R;

import static de.marmaro.krt.ffupdater.MainActivity.AVAILABLE_APPS_LOADER_ID;
import static de.marmaro.krt.ffupdater.MainActivity.TRIGGER_DOWNLOAD_FOR_APP;

/**
 * Created by Tobiwan on 23.08.2019.
 */
public class DownloadNewAppDialog extends DialogFragment {
    private final LoaderManager.LoaderCallbacks<AvailableApps> callbacks;

    public DownloadNewAppDialog(LoaderManager.LoaderCallbacks<AvailableApps> callbacks) {
        this.callbacks = callbacks;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Pair<List<App>, CharSequence[]> notInstalledApps = getNotInstalledApps();
        List<App> apps = notInstalledApps.getValue1();
        CharSequence[] options = notInstalledApps.getValue2();
        return new AlertDialog.Builder(getActivity())
                .setTitle("Download new app")
                .setItems(options, (dialog, which) -> {
                    App app = apps.get(which);
                    switch (app) {
                        case FENNEC_BETA:
                        case FENNEC_NIGHTLY:
                            showWarning(app);
                            break;
                        default:
                            triggerDownload(app);
                    }
                })
                .create();
    }

    private void showWarning(App app) {
        FragmentManager fragmentManager = Objects.requireNonNull(getFragmentManager());
        new WarningAppDialog(callbacks, app).show(fragmentManager, WarningAppDialog.TAG);
    }

    private void triggerDownload(App app) {
        FragmentActivity fragmentActivity = Objects.requireNonNull(getActivity());
        FragmentManager fragmentManager = Objects.requireNonNull(getFragmentManager());

        Bundle bundle = new Bundle();
        bundle.putString(TRIGGER_DOWNLOAD_FOR_APP, app.name());
        LoaderManager.getInstance(fragmentActivity).restartLoader(AVAILABLE_APPS_LOADER_ID, bundle, callbacks);
        new FetchDownloadUrlDialog().show(fragmentManager, FetchDownloadUrlDialog.TAG);
    }

    private Pair<List<App>, CharSequence[]> getNotInstalledApps() {
        Context context = Objects.requireNonNull(getContext());
        Activity activity = Objects.requireNonNull(getActivity());

        InstalledApps detector = new InstalledApps(activity.getPackageManager());
        List<App> notInstalledApps = detector.getNotInstalledApps();
        CharSequence[] notInstalledAppNames = new CharSequence[notInstalledApps.size()];

        for (int i = 0; i < notInstalledApps.size(); i++) {
            switch (notInstalledApps.get(i)) {
                case FENNEC_RELEASE:
                    notInstalledAppNames[i] = context.getString(R.string.fennecReleaseTitleText);
                    break;
                case FENNEC_BETA:
                    notInstalledAppNames[i] = context.getString(R.string.fennecBetaTitleText);
                    break;
                case FENNEC_NIGHTLY:
                    notInstalledAppNames[i] = context.getString(R.string.fennecNightlyTitleText);
                    break;
                case FIREFOX_KLAR:
                    notInstalledAppNames[i] = context.getString(R.string.firefoxKlarTitleText);
                    break;
                case FIREFOX_FOCUS:
                    notInstalledAppNames[i] = context.getString(R.string.firefoxFocusTitleText);
                    break;
                case FIREFOX_LITE:
                    notInstalledAppNames[i] = context.getString(R.string.firefoxLiteTitleText);
                    break;
                case FENIX:
                    notInstalledAppNames[i] = context.getString(R.string.fenixTitleText);
                    break;
            }
        }
        return new Pair<>(notInstalledApps, notInstalledAppNames);
    }

    private static class Pair<A, B> {
        private final A value1;
        private final B value2;

        Pair(A value1, B value2) {
            this.value1 = value1;
            this.value2 = value2;
        }

        A getValue1() {
            return value1;
        }

        B getValue2() {
            return value2;
        }
    }
}
