package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.app.fetch.ApiConsumer;
import de.marmaro.krt.ffupdater.app.fetch.mozillaci.MozillaCiConsumer;
import de.marmaro.krt.ffupdater.app.fetch.mozillaci.dao.Result;
import de.marmaro.krt.ffupdater.app.interfaces.BaseApp;
import de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult.FILE_HASH_SHA256;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
public class FirefoxFocus extends BaseApp {
    public static final String INSTALLED_VERSION_KEY =
            "device_app_register_FIREFOX_FOCUS_version_name";

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
        return getInstalledVersionFromSharedPreferences(context, INSTALLED_VERSION_KEY);
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
        final MozillaCiConsumer consumer = new MozillaCiConsumer(new ApiConsumer());
        final Result result = consumer.consume("project.mobile.focus.release.latest",
                String.format("app-focus-%s-release-unsigned.apk", getAbiAbbreviation(abi)));

        final String buildTimestamp = result.getTimestamp();
        final boolean updateAvailable = getInstalledVersion(context)
                .map(x -> !x.equals(buildTimestamp))
                .orElse(true);

        return new UpdateCheckResult.Builder()
                .setUpdateAvailable(updateAvailable)
                .setDownloadUrl(result.getUrl())
                .setVersion(buildTimestamp)
                .setMetadata(Map.of(FILE_HASH_SHA256, result.getHash()))
                .build();
    }

    private String getAbiAbbreviation(ABI abi) {
        switch (abi) {
            case AARCH64:
                return "aarch64";
            case ARM:
                return "arm";
            case X86:
            case X86_64:
                throw new ParamRuntimeException("unsupported ABI %s", abi);
            default:
                throw new ParamRuntimeException("unknown ABI %s - switch fallthrough", abi);
        }
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
        setInstalledVersionInSharedPreferences(context, INSTALLED_VERSION_KEY, installedVersion);
    }
}
