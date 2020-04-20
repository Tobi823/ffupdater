package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;

import org.apache.commons.codec.binary.ApacheCodecHex;

/**
 * All supported apps.
 */
public enum App {
    FENNEC_RELEASE(R.string.fennecReleaseTitleText,
            "org.mozilla.firefox",
            "A7:8B:62:A5:16:5B:44:94:B2:FE:AD:9E:76:A2:80:D2:2D:93:7F:EE:62:51:AE:CE:59:94:46:B2:EA:31:9B:04"),
    FENNEC_BETA(R.string.fennecBetaTitleText,
            "org.mozilla.firefox_beta",
            "A7:8B:62:A5:16:5B:44:94:B2:FE:AD:9E:76:A2:80:D2:2D:93:7F:EE:62:51:AE:CE:59:94:46:B2:EA:31:9B:04"),
    FENNEC_NIGHTLY(R.string.fennecNightlyTitleText,
            "org.mozilla.fennec_aurora",
            "A7:8B:62:A5:16:5B:44:94:B2:FE:AD:9E:76:A2:80:D2:2D:93:7F:EE:62:51:AE:CE:59:94:46:B2:EA:31:9B:04"),
    FIREFOX_KLAR(R.string.firefoxKlarTitleText,
            "org.mozilla.klar",
            "62:03:A4:73:BE:36:D6:4E:E3:7F:87:FA:50:0E:DB:C7:9E:AB:93:06:10:AB:9B:9F:A4:CA:7D:5C:1F:1B:4F:FC"),
    FIREFOX_FOCUS(R.string.firefoxFocusTitleText,
            "org.mozilla.focus",
            "62:03:A4:73:BE:36:D6:4E:E3:7F:87:FA:50:0E:DB:C7:9E:AB:93:06:10:AB:9B:9F:A4:CA:7D:5C:1F:1B:4F:FC"),
    FIREFOX_LITE(R.string.firefoxLiteTitleText,
            "org.mozilla.rocket",
            "86:3A:46:F0:97:39:32:B7:D0:19:9B:54:91:12:74:1C:2D:27:31:AC:72:EA:11:B7:52:3A:A9:0A:11:BF:56:91"),
    FENIX(R.string.fenixTitleText,
            "org.mozilla.fenix",
            "50:04:77:90:88:E7:F9:88:D5:BC:5C:C5:F8:79:8F:EB:F4:F8:CD:08:4A:1B:2A:46:EF:D4:C8:EE:4A:EA:F2:11");

    private final int titleId;
    private final String packageName;
    private final byte[] signatureHash;

    App(int titleId, String packageName, String signatureHash) {
        this.titleId = titleId;
        this.packageName = packageName;
        this.signatureHash = ApacheCodecHex.decodeHex(signatureHash.replaceAll(":", ""));
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
}
