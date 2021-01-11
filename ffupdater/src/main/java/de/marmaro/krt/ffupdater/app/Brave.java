package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.app.fetch.ApiConsumer;
import de.marmaro.krt.ffupdater.app.fetch.github.GithubConsumer;
import de.marmaro.krt.ffupdater.app.fetch.github.dao.Asset;
import de.marmaro.krt.ffupdater.app.fetch.github.dao.Result;
import de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult;
import de.marmaro.krt.ffupdater.device.ABI;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult.FILE_SIZE_BYTES;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;

/**
 * https://api.github.com/repos/brave/brave-browser/releases
 */
public class Brave extends BaseApp {

    @Override
    public String getPackageName() {
        return "com.brave.browser";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.brave_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.brave_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return Optional.of(context.getString(R.string.brave_warning));
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String getSignatureHashAsString() {
        return "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac";
    }

    @Override
    public String getDisplayDownloadSource(Context context) {
        return context.getString(R.string.github);
    }

    @Override
    public Optional<String> getDisplayInstalledVersion(Context context) {
        return getInstalledVersion(context);
    }

    @Override
    public Optional<String> getInstalledVersion(Context context) {
        return getInstalledVersionFromPackageManager(context);
    }

    @Override
    public int getMinApiLevel() {
        return Build.VERSION_CODES.N;
    }

    @Override
    public List<ABI> getSupportedAbi() {
        return Arrays.asList(AARCH64, ARM, X86_64, X86);
    }

    @Override
    public UpdateCheckResult updateCheck(Context context, ABI abi) {
        final Result result = new GithubConsumer.Builder()
                .setApiConsumer(new ApiConsumer())
                .setRepoOwner("brave")
                .setRepoName("brave-browser")
                .setResultsPerPage(20)
                .setValidReleaseTester(release -> !release.isPreRelease() &&
                        release.getAssets().stream().map(Asset::getName).anyMatch(name -> name.endsWith(".apk")))
                .setCorrectDownloadUrlTester(asset -> asset.getName().equals(getFileNameForAbi(abi)))
                .build()
                .updateCheck();

        final String version = result.getTagName().replace("v", "");
        final boolean update = getInstalledVersion(context).map(x -> !x.equals(version)).orElse(true);

        return new UpdateCheckResult.Builder()
                .setUpdateAvailable(update)
                .setDownloadUrl(result.getUrl())
                .setVersion(version)
                .setMetadata(Map.of(FILE_SIZE_BYTES, result.getFileSizeBytes()))
                .build();
    }

    private String getFileNameForAbi(ABI abi) {
        switch (abi) {
            case AARCH64:
                return "BraveMonoarm64.apk";
            case ARM:
                return "BraveMonoarm.apk";
            case X86:
                return "BraveMonox86.apk";
            case X86_64:
                return "BraveMonox64.apk";
            default:
                throw new ParamRuntimeException("Unknown ABI %s - switch fallthrough", abi);
        }
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
    }
}
