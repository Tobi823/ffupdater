package de.marmaro.krt.ffupdater.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        NotInstalledApps notInstalledApps = getNotInstalledApps();
        return new AlertDialog.Builder(getActivity())
                .setTitle("Download new app")
                .setItems(notInstalledApps.getAppNames(), (dialog, which) -> {
                    App app = notInstalledApps.getApps().get(which);
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
        Bundle bundle = new Bundle();
        bundle.putString(TRIGGER_DOWNLOAD_FOR_APP, app.name());

        FragmentActivity fragmentActivity = Objects.requireNonNull(getActivity());
        LoaderManager.getInstance(fragmentActivity).restartLoader(AVAILABLE_APPS_LOADER_ID, bundle, callbacks);

        FragmentManager fragmentManager = Objects.requireNonNull(getFragmentManager());
        new FetchDownloadUrlDialog().show(fragmentManager, FetchDownloadUrlDialog.TAG);
    }

    private NotInstalledApps getNotInstalledApps() {
        Activity activity = Objects.requireNonNull(getActivity());
        InstalledApps detector = new InstalledApps(activity.getPackageManager());
        List<App> notInstalledApps = detector.getNotInstalledApps();

        CharSequence[] appNames = new CharSequence[notInstalledApps.size()];
        int arrayIndex = 0;
        for (App notInstalledApp : notInstalledApps) {
            String titleText;
            switch (notInstalledApp) {
                case FENNEC_RELEASE:
                    titleText = activity.getString(R.string.fennecReleaseTitleText);
                    break;
                case FENNEC_BETA:
                    titleText = activity.getString(R.string.fennecBetaTitleText);
                    break;
                case FENNEC_NIGHTLY:
                    titleText = activity.getString(R.string.fennecNightlyTitleText);
                    break;
                case FIREFOX_KLAR:
                    titleText = activity.getString(R.string.firefoxKlarTitleText);
                    break;
                case FIREFOX_FOCUS:
                    titleText = activity.getString(R.string.firefoxFocusTitleText);
                    break;
                case FIREFOX_LITE:
                    titleText = activity.getString(R.string.firefoxLiteTitleText);
                    break;
                case FENIX:
                    titleText = activity.getString(R.string.fenixTitleText);
                    break;
                default:
                    throw new RuntimeException("missing switch statement");
            }
            appNames[arrayIndex++] = titleText;
        }
        return new NotInstalledApps(notInstalledApps, appNames);
    }

    private static class NotInstalledApps {
        private final List<App> apps;
        private final CharSequence[] appNames;

        NotInstalledApps(List<App> apps, CharSequence[] appNames) {
            this.apps = apps;
            this.appNames = appNames;
        }

        List<App> getApps() {
            return apps;
        }

        CharSequence[] getAppNames() {
            return appNames;
        }
    }
}
