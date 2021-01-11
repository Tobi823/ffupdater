package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.content.pm.PackageManager;
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
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.release.latest
 * https://www.apkmirror.com/apk/mozilla/firefox/
 */
public class FirefoxRelease extends BaseApp {

    @Override
    public String getPackageName() {
        return "org.mozilla.firefox";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.firefox_release_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.firefox_release_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return Optional.empty();
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
        return getInstalledVersion(context.getPackageManager());
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
    public UpdateCheckResult updateCheck(PackageManager pm, ABI abi) {
        return null; //TODO
    }
}
