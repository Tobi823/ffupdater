package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult;
import de.marmaro.krt.ffupdater.device.ABI;

import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-beta/
 */
public class FirefoxBeta extends BaseApp {

    @Override
    public String getPackageName() {
        return "org.mozilla.firefox_beta";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.firefox_beta_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.firefox_beta_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return Optional.of(context.getString(R.string.firefox_beta_warning));
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String getSignatureHashAsString() {
        return "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04";
    }

    @Override
    public String getDisplayDownloadSource(Context context) {
        return context.getString(R.string.mozilla_ci);
    }

    @Override
    public Optional<String> getDisplayInstalledVersion(Context context) {
        return getInstalledVersionFromPackageManager(context);
    }

    @Override
    public Optional<String> getInstalledVersion(Context context) {
        return getInstalledVersionFromSharedPreferences(context, "device_app_register_FIREFOX_BETA_version_name");
    }

    @Override
    public int getMinApiLevel() {
        return Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public List<ABI> getSupportedAbi() {
        return Arrays.asList(AARCH64, ARM, X86_64, X86);
    }

    @Override
    public UpdateCheckResult updateCheck(Context context, ABI abi) {
        return null; //TODO
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
    }
}
