package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;

import java.util.List;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.AvailableApps;
import de.marmaro.krt.ffupdater.InstalledAppsDetector;
import de.marmaro.krt.ffupdater.R;

import static de.marmaro.krt.ffupdater.MainActivity.AVAILABLE_APPS_LOADER_ID;
import static de.marmaro.krt.ffupdater.MainActivity.TRIGGER_DOWNLOAD_FOR_APP;

/**
 * Created by Tobiwan on 23.08.2019.
 */
public class DownloadNewAppDialog extends DialogFragment {

    private LoaderManager.LoaderCallbacks<AvailableApps> callbacks;

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
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App selectedApp = apps.get(which);
                        Bundle bundle = new Bundle();
                        bundle.putString(TRIGGER_DOWNLOAD_FOR_APP, selectedApp.name());
                        LoaderManager.getInstance(getActivity()).restartLoader(AVAILABLE_APPS_LOADER_ID, bundle, callbacks);
                        callbacks = null;
                        new FetchDownloadUrlDialog().show(getFragmentManager(), FetchDownloadUrlDialog.TAG);
                    }
                })
                .create();
    }

    private Pair<List<App>, CharSequence[]> getNotInstalledApps() {
        Context context = Preconditions.checkNotNull(getContext());

        InstalledAppsDetector detector = new InstalledAppsDetector(getActivity().getPackageManager());
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
        private A value1;
        private B value2;

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
