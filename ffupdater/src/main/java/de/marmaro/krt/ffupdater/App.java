package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.marmaro.krt.ffupdater.device.DeviceABI.ABI;

import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.X86;
import static de.marmaro.krt.ffupdater.device.DeviceABI.ABI.X86_64;

/**
 * All supported apps.
 * You can verify the APK certificate fingerprint on https://www.apkmirror.com and other sites.
 */
public enum App {
    FENNEC_RELEASE(R.string.fennecReleaseTitleText,
            R.string.fennec_release_description,
            R.string.empty,
            R.string.mozilla,
            "org.mozilla.firefox",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04",
            Collections.emptyList()),
    FENNEC_BETA(R.string.fennecBetaTitleText,
            R.string.fennec_beta_description,
            R.string.switch_to_unsafe_fennec_message,
            R.string.mozilla,
            "org.mozilla.firefox_beta",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04",
            Collections.emptyList()),
    FENNEC_NIGHTLY(R.string.fennecNightlyTitleText,
            R.string.fennec_nightly_description,
            R.string.switch_to_unsafe_fennec_message,
            R.string.mozilla,
            "org.mozilla.fennec_aurora",
            "bc0488838d06f4ca6bf32386daab0dd8ebcf3e7730787459f62fb3cd14a1baaa",
            Collections.emptyList()),
    FIREFOX_KLAR(R.string.firefoxKlarTitleText,
            R.string.firefox_klar_description,
            R.string.empty,
            R.string.github,
            "org.mozilla.klar",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc",
            Collections.emptyList()),
    FIREFOX_FOCUS(R.string.firefoxFocusTitleText,
            R.string.firefox_focus_description,
            R.string.empty,
            R.string.github,
            "org.mozilla.focus",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc",
            Arrays.asList(X86, X86_64)),
    FIREFOX_LITE(R.string.firefoxLiteTitleText,
            R.string.firefox_lite_description,
            R.string.empty,
            R.string.github,
            "org.mozilla.rocket",
            "863a46f0973932b7d0199b549112741c2d2731ac72ea11b7523aa90a11bf5691",
            Collections.emptyList()),
    FENIX(R.string.fenixTitleText,
            R.string.fenix_description,
            R.string.switch_to_unsafe_fenix_message,
            R.string.github,
            "org.mozilla.fenix",
            "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211",
            Collections.emptyList());

    private final int titleId;
    private final int descriptionId;
    private final int warningId;
    private final int downloadSourceId;
    private final String packageName;
    private final byte[] signatureHash;
    private final List<ABI> unsupportedAbis;

    App(int titleId, int descriptionId, int warningId, int downloadSourceId, String packageName, String signatureHash, List<ABI> unsupportedAbis) {
        this.titleId = titleId;
        this.descriptionId = descriptionId;
        this.warningId = warningId;
        this.downloadSourceId = downloadSourceId;
        this.packageName = packageName;
        this.signatureHash = ApacheCodecHex.decodeHex(signatureHash);
        this.unsupportedAbis = unsupportedAbis;
    }

    @NonNull
    public String getTitle(Context context) {
        return context.getString(titleId);
    }

    @NonNull
    public String getDescription(Context context) {
        return context.getString(descriptionId);
    }

    @NonNull
    public String getWarning(Context context) {
        return context.getString(warningId);
    }

    @NonNull
    public String getDownloadSource(Context context) {
        return context.getString(downloadSourceId);
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return SHA-256 hash of the APK signature
     */
    @NonNull
    public byte[] getSignatureHash() {
        return signatureHash;
    }

    @NonNull
    public List<ABI> getUnsupportedAbis() {
        return unsupportedAbis;
    }
}
