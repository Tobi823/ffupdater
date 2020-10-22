package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.util.Arrays;
import java.util.List;

import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static de.marmaro.krt.ffupdater.App.ReleaseIdType.TIMESTAMP;
import static de.marmaro.krt.ffupdater.App.ReleaseIdType.VERSION;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;

/**
 * All supported apps.
 * You can verify the APK certificate fingerprint on <a href="https://www.apkmirror.com">APKMirror</a> and other sites.
 */
@SuppressWarnings("SpellCheckingInspection")
public enum App {
    FIREFOX_KLAR(R.string.firefox_klar_title,
            R.string.firefox_klar_description,
            R.string.empty,
            R.string.mozilla_ci,
            "org.mozilla.klar",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc",
            Arrays.asList(AARCH64, ARM),
            21,
            TIMESTAMP),
    FIREFOX_FOCUS(R.string.firefox_focus_title,
            R.string.firefox_focus_description,
            R.string.empty,
            R.string.mozilla_ci,
            "org.mozilla.focus",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc",
            Arrays.asList(AARCH64, ARM),
            21,
            TIMESTAMP),
    FIREFOX_LITE(R.string.firefox_lite_title,
            R.string.firefox_lite_description,
            R.string.firefox_lite_warning,
            R.string.github,
            "org.mozilla.rocket",
            "863a46f0973932b7d0199b549112741c2d2731ac72ea11b7523aa90a11bf5691",
            Arrays.asList(AARCH64, ARM, X86_64, X86),
            21,
            VERSION),
    FIREFOX_RELEASE(R.string.firefox_release_title,
            R.string.firefox_release_description,
            R.string.empty,
            R.string.mozilla_ci,
            "org.mozilla.firefox",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04",
            Arrays.asList(AARCH64, ARM, X86_64, X86),
            21,
            TIMESTAMP),
    FIREFOX_BETA(R.string.firefox_beta_title,
            R.string.firefox_beta_description,
            R.string.firefox_beta_warning,
            R.string.mozilla_ci,
            "org.mozilla.firefox_beta",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04",
            Arrays.asList(AARCH64, ARM, X86_64, X86),
            21,
            TIMESTAMP),
    FIREFOX_NIGHTLY(R.string.firefox_nightly_title,
            R.string.firefox_nightly_description,
            R.string.firefox_nightly_warning,
            R.string.mozilla_ci,
            "org.mozilla.fenix",
            "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211",
            Arrays.asList(AARCH64, ARM, X86_64, X86),
            21,
            TIMESTAMP),
    LOCKWISE(R.string.lockwise_title,
            R.string.lockwise_description,
            R.string.lockwise_warning,
            R.string.github,
            "mozilla.lockbox",
            "64d26b507078deba2fee42d6bd0bfad41d39ffc4e791f281028e5e73d3c8d2f2",
            Arrays.asList(AARCH64, ARM, X86_64, X86),
            24,
            VERSION),
    BRAVE(R.string.brave_title,
            R.string.brave_description,
            R.string.brave_warning,
            R.string.github,
            "com.brave.browser",
            "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac",
            Arrays.asList(AARCH64, ARM, X86_64, X86),
            24,
            VERSION);

    private final int titleId;
    private final int descriptionId;
    private final int warningId;
    private final int downloadSourceId;
    private final String packageName;
    private final byte[] signatureHash;
    private final List<ABI> supportedAbis;
    private final int minApiLevel;
    private final ReleaseIdType releaseIdType;

    App(int titleId, int descriptionId, int warningId, int downloadSourceId, String packageName,
        String signatureHash, List<ABI> supportedAbis, int minApiLevel, ReleaseIdType releaseIdType) {
        this.titleId = titleId;
        this.descriptionId = descriptionId;
        this.warningId = warningId;
        this.downloadSourceId = downloadSourceId;
        this.packageName = packageName;
        this.signatureHash = ApacheCodecHex.decodeHex(signatureHash);
        this.supportedAbis = supportedAbis;
        this.minApiLevel = minApiLevel;
        this.releaseIdType = releaseIdType;
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

    public int getMinApiLevel() {
        return minApiLevel;
    }

    /**
     * @param deviceEnvironment device metadata
     * @return is the app not available for the device's ABI?
     */
    public boolean isCompatibleWithDeviceAbi(DeviceEnvironment deviceEnvironment) {
        return supportedAbis.stream().anyMatch(abi -> deviceEnvironment.getSupportedAbis().contains(abi));
    }

    /**
     * @param deviceEnvironment device metadata
     * @return is the app not compatible with the device's API Level?
     */
    public boolean isCompatibleWithDeviceApiLevel(DeviceEnvironment deviceEnvironment) {
        return deviceEnvironment.getApiLevel() >= minApiLevel;
    }

    public enum ReleaseIdType {
        VERSION,
        TIMESTAMP
    }

    public ReleaseIdType getReleaseIdType() {
        return releaseIdType;
    }
}
