package de.marmaro.krt.ffupdater;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.fragment.app.DialogFragment;

import java.util.List;

/**
 * Created by Tobiwan on 23.08.2019.
 */
public class DownloadNewAppDialog extends DialogFragment {

    private AvailableApps availableApps;

    public DownloadNewAppDialog(AvailableApps availableApps) {
        this.availableApps = availableApps;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        Pair<List<App>, CharSequence[]> notInstalledApps = getNotInstalledApps();
        List<App> apps = notInstalledApps.getValue1();
        CharSequence[] options = notInstalledApps.getValue2();
        return new AlertDialog.Builder(getActivity())
                .setTitle("Download new app")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App selectedApp = apps.get(which);
                        String downloadUrl = availableApps.getDownloadUrl(selectedApp);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(downloadUrl));
                        startActivity(intent);
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
                case FENNEC_BETA:
                    notInstalledAppNames[i] = context.getString(R.string.fennecBetaTitleText);
                case FENNEC_NIGHTLY:
                    notInstalledAppNames[i] = context.getString(R.string.fennecNightlyTitleText);
                case FIREFOX_KLAR:
                    notInstalledAppNames[i] = context.getString(R.string.firefoxKlarTitleText);
                case FIREFOX_FOCUS:
                    notInstalledAppNames[i] = context.getString(R.string.firefoxFocusTitleText);
                case FIREFOX_LITE:
                    notInstalledAppNames[i] = context.getString(R.string.firefoxLiteTitleText);
                case FENIX:
                    notInstalledAppNames[i] = context.getString(R.string.fenixTitleText);
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
