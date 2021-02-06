package de.marmaro.krt.ffupdater.app.impl;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer;
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer;
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.dao.Result;
import de.marmaro.krt.ffupdater.app.BaseApp;
import de.marmaro.krt.ffupdater.app.UpdateCheckResult;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static de.marmaro.krt.ffupdater.app.impl.FirefoxRelease.APK_ARTIFACT;
import static de.marmaro.krt.ffupdater.app.impl.FirefoxRelease.TASK_NAME;
import static de.marmaro.krt.ffupdater.app.UpdateCheckResult.FILE_HASH_SHA256;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
public class FirefoxNightly extends BaseApp {
    public static final String INSTALLED_VERSION_KEY = "device_app_register_FIREFOX_NIGHTLY_version_name";

    @Override
    public String getPackageName() {
        return "org.mozilla.fenix";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.firefox_nightly_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.firefox_nightly_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return Optional.of(context.getString(R.string.firefox_nightly_warning));
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String getSignatureHashAsString() {
        return "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211";
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
        return Arrays.asList(AARCH64, ARM, X86_64, X86);
    }

    @Override
    public UpdateCheckResult updateCheck(Context context, ABI abi) {
        final MozillaCiConsumer consumer = new MozillaCiConsumer(new ApiConsumer());
        final String abiAbbreviation = getAbiAbbreviation(abi);
        final String task = String.format(TASK_NAME, abiAbbreviation);
        final String apkArtifact = String.format(APK_ARTIFACT, abiAbbreviation);
        final Result result = consumer.consume(task, apkArtifact);

        final String version = result.getTimestamp();
        final boolean update = getInstalledVersion(context).map(x -> !x.equals(version)).orElse(true);

        return new UpdateCheckResult.Builder()
                .setUpdateAvailable(update)
                .setDownloadUrl(result.getUrl())
                .setVersion(version)
                .setMetadata(Map.of(FILE_HASH_SHA256, result.getHash()))
                .build();
    }

    private String getAbiAbbreviation(ABI abi) {
        switch (abi) {
            case AARCH64:
                return "arm64-v8a";
            case ARM:
                return "armeabi-v7a";
            case X86:
                return "x86";
            case X86_64:
                return "x86_64";
            default:
                throw new ParamRuntimeException("unknown ABI %s - switch fallthrough", abi);
        }
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
        setInstalledVersionInSharedPreferences(context, INSTALLED_VERSION_KEY, installedVersion);
    }
}
