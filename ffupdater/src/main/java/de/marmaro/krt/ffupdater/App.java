package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;

import org.apache.commons.codec.binary.ApacheCodecHex;

/**
 * All supported apps.
 * You can verify the APK certificate fingerprint on https://www.apkmirror.com and other sites.
 */
public enum App {
    FENNEC_RELEASE(R.string.fennecReleaseTitleText,
            "org.mozilla.firefox",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"),
    FENNEC_BETA(R.string.fennecBetaTitleText,
            "org.mozilla.firefox_beta",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"),
    FENNEC_NIGHTLY(R.string.fennecNightlyTitleText,
            "org.mozilla.fennec_aurora",
            "bc0488838d06f4ca6bf32386daab0dd8ebcf3e7730787459f62fb3cd14a1baaa"),
    FIREFOX_KLAR(R.string.firefoxKlarTitleText,
            "org.mozilla.klar",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"),
    FIREFOX_FOCUS(R.string.firefoxFocusTitleText,
            "org.mozilla.focus",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"),
    FIREFOX_LITE(R.string.firefoxLiteTitleText,
            "org.mozilla.rocket",
            "863a46f0973932b7d0199b549112741c2d2731ac72ea11b7523aa90a11bf5691"),
    FENIX(R.string.fenixTitleText,
            "org.mozilla.fenix",
            "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211");

    private final int titleId;
    private final String packageName;
    private final byte[] signatureHash;

    App(int titleId, String packageName, String signatureHash) {
        this.titleId = titleId;
        this.packageName = packageName;
        this.signatureHash = ApacheCodecHex.decodeHex(signatureHash);
    }

    public String getTitle(Context context) {
        return context.getString(titleId);
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return SHA-256 hash of the APK signature
     */
    public byte[] getSignatureHash() {
        return signatureHash;
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

    public String getDownloadSource() {
        if (this == FENNEC_RELEASE || this == FENNEC_BETA || this == FENNEC_NIGHTLY) {
            return "Mozilla";
        }
        return "Github";
    }
}
