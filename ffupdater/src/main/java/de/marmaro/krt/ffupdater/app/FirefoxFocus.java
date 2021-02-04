package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.app.interfaces.BaseApp;
import de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult;
import de.marmaro.krt.ffupdater.device.ABI;

import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
public class FirefoxFocus extends BaseApp {

    @Override
    public String getPackageName() {
        return "org.mozilla.focus";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.firefox_focus_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.firefox_focus_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String getSignatureHashAsString() {
        return "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc";
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
        return getInstalledVersionFromSharedPreferences(context, "device_app_register_FIREFOX_FOCUS_version_name");
    }

    @Override
    public int getMinApiLevel() {
        return Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public List<ABI> getSupportedAbi() {
        return Arrays.asList(AARCH64, ARM);
    }

    @Override
    public UpdateCheckResult updateCheck(Context context, ABI abi) {
        return null; //TODO
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
    }
}
