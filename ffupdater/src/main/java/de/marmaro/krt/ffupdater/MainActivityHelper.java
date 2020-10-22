package de.marmaro.krt.ffupdater;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

public class MainActivityHelper {
    private final Activity activity;

    public MainActivityHelper(Activity activity) {
        this.activity = activity;
    }

    public CardView getAppCardViewForApp(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return activity.findViewById(R.id.firefoxKlarCard);
            case FIREFOX_FOCUS:
                return activity.findViewById(R.id.firefoxFocusCard);
            case FIREFOX_LITE:
                return activity.findViewById(R.id.firefoxLiteCard);
            case FIREFOX_RELEASE:
                return activity.findViewById(R.id.firefoxReleaseCard);
            case FIREFOX_BETA:
                return activity.findViewById(R.id.firefoxBetaCard);
            case FIREFOX_NIGHTLY:
                return activity.findViewById(R.id.firefoxNightlyCard);
            case LOCKWISE:
                return activity.findViewById(R.id.lockwiseCard);
            case BRAVE:
                return activity.findViewById(R.id.braveCard);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    public void enableDownloadButton(App app) {
        getDownloadButtonForApp(app).setImageResource(R.drawable.ic_file_download_orange);
    }

    public void disableDownloadButton(App app) {
        getDownloadButtonForApp(app).setImageResource(R.drawable.ic_file_download_grey);
    }

    public void registerDownloadButtonOnClickListener(App app, View.OnClickListener listener) {
        getDownloadButtonForApp(app).setOnClickListener(listener);
    }

    private ImageButton getDownloadButtonForApp(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return activity.findViewById(R.id.firefoxKlarDownloadButton);
            case FIREFOX_FOCUS:
                return activity.findViewById(R.id.firefoxFocusDownloadButton);
            case FIREFOX_LITE:
                return activity.findViewById(R.id.firefoxLiteDownloadButton);
            case FIREFOX_RELEASE:
                return activity.findViewById(R.id.firefoxReleaseDownloadButton);
            case FIREFOX_BETA:
                return activity.findViewById(R.id.firefoxBetaDownloadButton);
            case FIREFOX_NIGHTLY:
                return activity.findViewById(R.id.firefoxNightlyDownloadButton);
            case LOCKWISE:
                return activity.findViewById(R.id.lockwiseDownloadButton);
            case BRAVE:
                return activity.findViewById(R.id.braveDownloadButton);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    public void setInstalledVersionText(App app, String text) {
        getInstalledVersionTextViewForApp(app).setText(text);
    }

    private TextView getInstalledVersionTextViewForApp(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return activity.findViewById(R.id.firefoxKlarInstalledVersion);
            case FIREFOX_FOCUS:
                return activity.findViewById(R.id.firefoxFocusInstalledVersion);
            case FIREFOX_LITE:
                return activity.findViewById(R.id.firefoxLiteInstalledVersion);
            case FIREFOX_RELEASE:
                return activity.findViewById(R.id.firefoxReleaseInstalledVersion);
            case FIREFOX_BETA:
                return activity.findViewById(R.id.firefoxBetaInstalledVersion);
            case FIREFOX_NIGHTLY:
                return activity.findViewById(R.id.firefoxNightlyInstalledVersion);
            case LOCKWISE:
                return activity.findViewById(R.id.lockwiseInstalledVersion);
            case BRAVE:
                return activity.findViewById(R.id.braveInstalledVersion);
            default:
                throw new ParamRuntimeException("unknown installed version text view for app %s", app);
        }
    }

    public void setAvailableVersionText(App app, String text) {
        getAvailableVersionTextViewForApp(app).setText(text);
    }

    private TextView getAvailableVersionTextViewForApp(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return activity.findViewById(R.id.firefoxKlarAvailableVersion);
            case FIREFOX_FOCUS:
                return activity.findViewById(R.id.firefoxFocusAvailableVersion);
            case FIREFOX_LITE:
                return activity.findViewById(R.id.firefoxLiteAvailableVersion);
            case FIREFOX_RELEASE:
                return activity.findViewById(R.id.firefoxReleaseAvailableVersion);
            case FIREFOX_BETA:
                return activity.findViewById(R.id.firefoxBetaAvailableVersion);
            case FIREFOX_NIGHTLY:
                return activity.findViewById(R.id.firefoxNightlyAvailableVersion);
            case LOCKWISE:
                return activity.findViewById(R.id.lockwiseAvailableVersion);
            case BRAVE:
                return activity.findViewById(R.id.braveAvailableVersion);
            default:
                throw new ParamRuntimeException("unknown available version text view for app %s", app);
        }
    }

    public void registerInfoButtonOnClickListener(App app, View.OnClickListener listener) {
        getInfoButtonForApp(app).setOnClickListener(listener);
    }

    private View getInfoButtonForApp(App app) {
        switch (app) {
            case FIREFOX_KLAR:
                return activity.findViewById(R.id.firefoxKlarInfoButton);
            case FIREFOX_FOCUS:
                return activity.findViewById(R.id.firefoxFocusInfoButton);
            case FIREFOX_LITE:
                return activity.findViewById(R.id.firefoxLiteInfoButton);
            case FIREFOX_RELEASE:
                return activity.findViewById(R.id.firefoxReleaseInfoButton);
            case FIREFOX_BETA:
                return activity.findViewById(R.id.firefoxBetaInfoButton);
            case FIREFOX_NIGHTLY:
                return activity.findViewById(R.id.firefoxNightlyInfoButton);
            case LOCKWISE:
                return activity.findViewById(R.id.lockwiseInfoButton);
            case BRAVE:
                return activity.findViewById(R.id.braveInfoButton);
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }
}
