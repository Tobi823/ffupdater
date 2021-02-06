package de.marmaro.krt.ffupdater.app;

import android.content.Context;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import de.marmaro.krt.ffupdater.device.ABI;

class CacheWrapper implements App {
    public static final long CACHE_TIME = Duration.ofMinutes(10).toMillis();

    private final App app;
    private UpdateCheckResult cache = null;
    private long cacheTimestamp = 0;

    public CacheWrapper(App app) {
        this.app = app;
    }

    @Override
    public UpdateCheckResult updateCheckBlocking(Context context, ABI abi) {
        final long cacheAge = System.currentTimeMillis() - cacheTimestamp;
        if (cacheAge > CACHE_TIME || cache == null) {
            cache = app.updateCheckBlocking(context, abi);
            cacheTimestamp = System.currentTimeMillis();
        }
        return cache;
    }

    public App getWrappedApp() {
        return app;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return app.getDisplayTitle(context);
    }

    @Override
    public String getDisplayDescription(Context context) {
        return app.getDisplayDescription(context);
    }

    @Override
    public Optional<String> getDisplayWarning(Context context) {
        return app.getDisplayWarning(context);
    }

    @Override
    public Optional<String> getDisplayInstalledVersion(Context context) {
        return app.getDisplayInstalledVersion(context);
    }

    @Override
    public String getDisplayDownloadSource(Context context) {
        return app.getDisplayDownloadSource(context);
    }

    @Override
    public boolean isInstalled(Context context) {
        return app.isInstalled(context);
    }

    @Override
    public Optional<String> getInstalledVersion(Context context) {
        return app.getInstalledVersion(context);
    }

    @Override
    public String getPackageName() {
        return app.getPackageName();
    }

    @Override
    public byte[] getSignatureHash() {
        return app.getSignatureHash();
    }

    @Override
    public String getSignatureHashAsString() {
        return app.getSignatureHashAsString();
    }

    @Override
    public void installationCallback(Context context, String installedVersion) {
        app.installationCallback(context, installedVersion);
    }

    @Override
    public int getMinApiLevel() {
        return app.getMinApiLevel();
    }

    @Override
    public List<ABI> getSupportedAbi() {
        return app.getSupportedAbi();
    }
}
