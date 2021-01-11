package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.content.pm.PackageManager;
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

import static de.marmaro.krt.ffupdater.app.interfaces.UpdateCheckResult.FILE_SIZE_BYTES;
import static de.marmaro.krt.ffupdater.device.ABI.AARCH64;
import static de.marmaro.krt.ffupdater.device.ABI.ARM;
import static de.marmaro.krt.ffupdater.device.ABI.X86;
import static de.marmaro.krt.ffupdater.device.ABI.X86_64;

/**
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases
 */
public class Lockwise extends BaseApp {
    @Override
    public String getPackageName() {
        return "mozilla.lockbox";
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.lockwise_title);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return context.getString(R.string.lockwise_description);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return Optional.of(context.getString(R.string.lockwise_warning));
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public String getSignatureHashAsString() {
        return "64d26b507078deba2fee42d6bd0bfad41d39ffc4e791f281028e5e73d3c8d2f2";
    }

    @Override
    public String getDisplayDownloadSource(Context context) {
        return context.getString(R.string.github);
    }

    @Override
    public Optional<String> getDisplayInstalledVersion(Context context) {
        return getInstalledVersion(context.getPackageManager());
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
    public UpdateCheckResult updateCheck(PackageManager pm, ABI abi) {
        final Result result = new GithubConsumer.Builder()
                .setApiConsumer(new ApiConsumer())
                .setRepoOwner("mozilla-lockwise")
                .setRepoName("lockwise-android")
                .setResultsPerPage(5)
                .setValidReleaseTester(release -> !release.isPreRelease() &&
                        release.getAssets().stream().map(Asset::getName).anyMatch(name -> name.endsWith(".apk")))
                // each release have only one .apk file
                .setCorrectDownloadUrlTester(asset -> asset.getName().endsWith(".apk"))
                .build()
                .updateCheck();

        // tag_name can be: "release-v4.0.3", "release-v4.0.0-RC-2"
        final String version = result.getTagName().split("v")[1].split("-")[0];
        final boolean update = getInstalledVersion(pm).map(x -> !x.equals(version)).orElse(false);

        return new UpdateCheckResult.Builder()
                .setUpdateAvailable(update)
                .setDownloadUrl(result.getUrl())
                .setVersion(version)
                .setDisplayVersion(version)
                .setMetadata(Map.of(FILE_SIZE_BYTES, result.getFileSizeBytes()))
                .build();
    }
}
