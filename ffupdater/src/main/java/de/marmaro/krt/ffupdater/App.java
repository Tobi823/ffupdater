package de.marmaro.krt.ffupdater;

import android.content.Context;

/**
 * All supported apps.
 */
public enum App {
    FENNEC_RELEASE(R.string.fennecReleaseTitleText),
    FENNEC_BETA(R.string.fennecBetaTitleText),
    FENNEC_NIGHTLY(R.string.fennecNightlyTitleText),
    FIREFOX_KLAR(R.string.firefoxKlarTitleText),
    FIREFOX_FOCUS(R.string.firefoxFocusTitleText),
    FIREFOX_LITE(R.string.firefoxLiteTitleText),
    FENIX(R.string.fenixTitleText);

    int titleId;

    App(int titleId) {
        this.titleId = titleId;
    }

    public String getTitle(Context context) {
        return context.getString(titleId);
    }

    public String getDescription(Context context) {
        switch (this) {
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
            default:
                throw new RuntimeException("missing switch statement");
        }
    }
}
