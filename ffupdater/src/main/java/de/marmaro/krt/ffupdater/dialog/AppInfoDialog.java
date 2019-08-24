package de.marmaro.krt.ffupdater.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.fragment.app.DialogFragment;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.R;

/**
 * Created by Tobiwan on 23.08.2019.
 */
public class AppInfoDialog extends DialogFragment {

    private App app;

    public AppInfoDialog(App app) {
        this.app = app;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        return new AlertDialog.Builder(getActivity())
                .setTitle(getTitle())
                .setMessage(getText())
                .create();
    }

    private String getText() {
        Context context = Preconditions.checkNotNull(getContext());
        switch (app) {
            case FENNEC_RELEASE:
                return "This is the default and well-known Firefox browser from Mozilla.\n\nThe app will be downloaded from the official Mozilla API.";
            case FENNEC_BETA:
                return "This app is a prerelease of the Firefox browser and is designed for developers and testers. It may contain various bugs that affect your usual work with the browser.\n\nThe app will be downloaded from the official Mozilla API.";
            case FENNEC_NIGHTLY:
                return "This app is a bleeding edge release of the Firefox browser and is designed for developers and testers. It may contain various bugs that affect your usual work with the browser.\n\nThe app will be downloaded from the official Mozilla API.";
            case FIREFOX_KLAR:
                return "This is a modification of the Firefox browser aimed to increase privacy. Firefox Klar is for Germany, Switzerland and Austria and almost identical to Firefox Focus. Only the name was change to avoid confusion with the magazine 'Focus'.\n\nThe app will be downloaded from Github - only updates which are released on Github can be downloaded.";
            case FIREFOX_FOCUS:
                return "This is a modification of the Firefox browser aimed to increase privacy.\n\nThe app will be downloaded from Github - only updates which are released on Github can be downloaded.";
            case FIREFOX_LITE:
                return "Firefox Lite is primary developed for users in Indonesia. The app is very small and supports techniques to reduce (mobile) data consumption.\n\nThe app will be downloaded from Github - only updates which are released on Github can be downloaded.";
            case FENIX:
                return "'Firefox Preview (internal code name: 'Fenix') is an all-new browser for Android'. This app is currently under development and contains more trackers than usual to analyse/improve the app usage.\n\nThe app will be downloaded from Github - only updates which are released on Github can be downloaded.";
        }
        throw new IllegalArgumentException("unknown enum value");
    }

    private CharSequence getTitle() {
        Context context = Preconditions.checkNotNull(getContext());
        switch (app) {
            case FENNEC_RELEASE:
                return context.getString(R.string.fennecReleaseTitleText);
            case FENNEC_BETA:
                return context.getString(R.string.fennecBetaTitleText);
            case FENNEC_NIGHTLY:
                return context.getString(R.string.fennecNightlyTitleText);
            case FIREFOX_KLAR:
                return context.getString(R.string.firefoxKlarTitleText);
            case FIREFOX_FOCUS:
                return context.getString(R.string.firefoxFocusTitleText);
            case FIREFOX_LITE:
                return context.getString(R.string.firefoxLiteTitleText);
            case FENIX:
                return context.getString(R.string.fenixTitleText);
        }
        throw new IllegalArgumentException("unknown enum value");
    }
}
