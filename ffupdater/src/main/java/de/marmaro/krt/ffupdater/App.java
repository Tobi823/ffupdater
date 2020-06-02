package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI;

import static de.marmaro.krt.ffupdater.App.CompareMethod.TIMESTAMP;
import static de.marmaro.krt.ffupdater.App.CompareMethod.VERSION;
import static de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI.X86;
import static de.marmaro.krt.ffupdater.device.DeviceEnvironment.ABI.X86_64;

/**
 * All supported apps.
 * You can verify the APK certificate fingerprint on <a href="https://www.apkmirror.com">APKMirror</a> and other sites.
 */
public enum App {
    @SuppressWarnings("SpellCheckingInspection")
    FENNEC_RELEASE(R.string.fennec_release_title,
            R.string.fennec_release_description,
            R.string.empty,
            R.string.mozilla,
            "org.mozilla.firefox",
            "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04",
            Collections.emptyList(),
            16,
            VERSION),
    @SuppressWarnings("SpellCheckingInspection")
    FIREFOX_KLAR(R.string.firefox_klar_title,
            R.string.firefox_klar_description,
            R.string.empty,
            R.string.mozilla_ci,
            "org.mozilla.klar",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc",
            Arrays.asList(X86, X86_64),
            21,
            TIMESTAMP),
    @SuppressWarnings("SpellCheckingInspection")
    FIREFOX_FOCUS(R.string.firefox_focus_title,
            R.string.firefox_focus_description,
            R.string.empty,
            R.string.mozilla_ci,
            "org.mozilla.focus",
            "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc",
            Arrays.asList(X86, X86_64),
            21,
            TIMESTAMP),
    FIREFOX_LITE(R.string.firefox_lite_title,
            R.string.firefox_lite_description,
            R.string.firefox_lite_warning,
            R.string.github,
            "org.mozilla.rocket",
            "863a46f0973932b7d0199b549112741c2d2731ac72ea11b7523aa90a11bf5691",
            Collections.emptyList(),
            21,
            VERSION),
    @SuppressWarnings("SpellCheckingInspection")
    FENIX_RELEASE(R.string.fenix_release_title,
            R.string.fenix_release_description,
            R.string.empty,
            R.string.mozilla_ci,
            "org.mozilla.fenix",
            "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211",
            Collections.emptyList(),
            21,
            TIMESTAMP),
    FENIX_BETA(R.string.fenix_beta_title,
            R.string.fenix_beta_description,
            R.string.fenix_beta_warning,
            R.string.mozilla_ci,
            "org.mozilla.fenix.beta",
            "f562c08f30778686d2a47b858f45e9ef357083085cb2891a96c409f360e9cab9",
            Collections.emptyList(),
            21,
            TIMESTAMP),
    @SuppressWarnings("SpellCheckingInspection")
    FENIX_NIGHTLY(R.string.fenix_nightly_title,
            R.string.fenix_nightly_description,
            R.string.fenix_nightly_warning,
            R.string.mozilla_ci,
            "org.mozilla.fenix.nightly",
            "77eac4ceed36afefba76179931dd4cc195ab0cd54baf355d215e7bbdd28e402a",
            Collections.emptyList(),
            21,
            TIMESTAMP),
    LOCKWISE(R.string.lockwise_title,
            R.string.lockwise_description,
            R.string.lockwise_warning,
            R.string.github,
            "mozilla.lockbox",
            "64d26b507078deba2fee42d6bd0bfad41d39ffc4e791f281028e5e73d3c8d2f2",
            Collections.emptyList(),
            24,
            VERSION);

    private final int titleId;
    private final int descriptionId;
    private final int warningId;
    private final int downloadSourceId;
    private final String packageName;
    private final byte[] signatureHash;
    private final List<ABI> unsupportedAbis;
    private final int minApiLevel;
    private final CompareMethod compareMethod;

    App(int titleId, int descriptionId, int warningId, int downloadSourceId, String packageName,
        String signatureHash, List<ABI> unsupportedAbis, int minApiLevel, CompareMethod compareMethod) {
        this.titleId = titleId;
        this.descriptionId = descriptionId;
        this.warningId = warningId;
        this.downloadSourceId = downloadSourceId;
        this.packageName = packageName;
        this.signatureHash = ApacheCodecHex.decodeHex(signatureHash);
        this.unsupportedAbis = unsupportedAbis;
        this.minApiLevel = minApiLevel;
        this.compareMethod = compareMethod;
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

    @NonNull
    public CompareMethod getCompareMethod() {
        return compareMethod;
    }

    /**
     * @param deviceABI device metadata
     * @return can the app be installed on the device?
     */
    public boolean isCompatibleWithDevice(DeviceEnvironment deviceABI) {
        return !isIncompatibleWithDeviceAbi(deviceABI) && !isIncompatibleWithDeviceApiLevel(deviceABI);
    }

    /**
     * @param deviceABI device metadata
     * @return is the app not available for the device's ABI?
     */
    public boolean isIncompatibleWithDeviceAbi(DeviceEnvironment deviceABI) {
        return unsupportedAbis.contains(deviceABI.getBestSuitedAbi());
    }

    /**
     * @param deviceABI device metadata
     * @return is the app not compatible with the device's API Level?
     */
    public boolean isIncompatibleWithDeviceApiLevel(DeviceEnvironment deviceABI) {
        return !deviceABI.isSdkIntEqualOrHigher(minApiLevel);
    }

    public enum CompareMethod {
        VERSION,
        TIMESTAMP
    }

    public CompareMethod getCompareMethodForUpdateCheck() {
        return compareMethod;
    }
}
