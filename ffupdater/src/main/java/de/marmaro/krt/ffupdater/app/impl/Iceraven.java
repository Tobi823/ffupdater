package de.marmaro.krt.ffupdater.app.impl;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer;
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer;
import de.marmaro.krt.ffupdater.app.impl.fetch.github.dao.Asset;
import de.marmaro.krt.ffupdater.app.impl.fetch.github.dao.Result;
import de.marmaro.krt.ffupdater.app.BaseApp;
import de.marmaro.krt.ffupdater.app.UpdateCheckResult;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static de.marmaro.krt.ffupdater.app.UpdateCheckResult.FILE_SIZE_BYTES;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;

/**
 * https://api.github.com/repos/fork-maintainers/iceraven-browser/releases
 */
public class Iceraven extends BaseApp {

    @Override
    public String getPackageName() {
        return "io.github.forkmaintainers.iceraven";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.iceraven_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.iceraven_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        //TODO
        return Optional.empty();
    }

    @Override
    public String getSignatureHashAsString() {
        return "9c0d22379f487b70a4f9f8bec0173cf91a1644f08f93385b5b782ce37660ba81";
    }

    @Override
    public String getDisplayDownloadSource(Context context) {
        return context.getString(R.string.github);
    }

    @Override
    public Optional<String> getDisplayInstalledVersion(Context context) {
        return getInstalledVersionFromPackageManager(context);
    }

    @Override
    public Optional<String> getInstalledVersion(Context context) {
        return getInstalledVersionFromPackageManager(context);
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
    public UpdateCheckResult updateCheckBlocking(Context context, ABI abi) {
        final Result result = new GithubConsumer.Builder()
                .setApiConsumer(new ApiConsumer())
                .setRepoOwner("fork-maintainers")
                .setRepoName("iceraven-browser")
                .setResultsPerPage(3)
                .setValidReleaseTester(release -> !release.isPreRelease() &&
                        release.getAssets().stream().map(Asset::getName).anyMatch(name -> name.endsWith(".apk")))
                .setCorrectDownloadUrlTester(asset -> asset.getName().endsWith(getFileSuffixForAbi(abi)))
                .build()
                .updateCheck();

        final String version = result.getTagName().replace("iceraven-", "");
        final boolean update = getInstalledVersion(context).map(x -> !x.equals(version)).orElse(true);

        return new UpdateCheckResult.Builder()
                .setUpdateAvailable(update)
                .setDownloadUrl(result.getUrl())
                .setVersion(version)
                .setMetadata(Map.of(FILE_SIZE_BYTES, result.getFileSizeBytes()))
                .build();
    }

    private String getFileSuffixForAbi(ABI abi) {
        switch (abi) {
            case AARCH64:
                return "browser-arm64-v8a-forkRelease.apk";
            case ARM:
                return "browser-armeabi-v7a-forkRelease.apk";
            case X86:
                return "browser-x86-forkRelease.apk";
            case X86_64:
                return "browser-x86_64-forkRelease.apk";
            default:
                throw new ParamRuntimeException("Unknown ABI %s - switch fallthrough", abi);
        }
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
    }
}
