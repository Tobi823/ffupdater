package de.marmaro.krt.ffupdater.version;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.device.InstalledApps;

import static de.marmaro.krt.ffupdater.App.CompareMethod.TIMESTAMP;
import static de.marmaro.krt.ffupdater.App.CompareMethod.VERSION;
import static de.marmaro.krt.ffupdater.App.FIREFOX_BETA;
import static de.marmaro.krt.ffupdater.App.FIREFOX_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FIREFOX_RELEASE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;
import static de.marmaro.krt.ffupdater.App.LOCKWISE;

/**
 * Helps to fetch the latest version name of apps with background threads.
 */
public class AvailableVersions {
    // StrictMode needs for every running thread a stats id
    private static final int TRAFFIC_FOCUS = 1002;
    private static final int TRAFFIC_KLAR = 1003;
    private static final int TRAFFIC_LITE = 1004;
    private static final int TRAFFIC_FIREFOX_RELEASE = 1005;
    private static final int TRAFFIC_FIREFOX_BETA = 1006;
    private static final int TRAFFIC_FIREFOX_NIGHTLY = 1007;
    private static final int TRAFFIC_LOCKWISE = 1008;
    private static final int NUMBER_BACKGROUND_THREADS = 7 + 1;

    private final ExecutorService executorService;
    private final PackageManager packageManager;
    private final Queue<Future> futures = new ConcurrentLinkedQueue<>();
    private final MetadataCache metadataStorage;
    private final DeviceEnvironment deviceABI;

    public AvailableVersions(Context context) {
        this(Preconditions.checkNotNull(
                context.getPackageManager()),
                PreferenceManager.getDefaultSharedPreferences(context),
                new DeviceEnvironment());
    }

    AvailableVersions(PackageManager packageManager, SharedPreferences sharedPreferences, DeviceEnvironment deviceABI) {
        this.executorService = Executors.newFixedThreadPool(NUMBER_BACKGROUND_THREADS);
        this.packageManager = packageManager;
        this.metadataStorage = new MetadataCache(sharedPreferences);
        this.deviceABI = deviceABI;
    }

    /**
     * Call this method for destructing AvailableVersions correctly to avoid memory leaks.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Check with {@code isUpdateAvailable} if any installed app is outdated.
     *
     * @return if one or more installed apps can be updated
     */
    public boolean areUpdatesForInstalledAppsAvailable() {
        for (App app : InstalledApps.getInstalledApps(packageManager)) {
            if (isUpdateAvailable(app)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compare the version name from the installed app with the latest version name from the developer (cached by
     * {@code checkUpdateForApp} or {@code checkUpdatesForInstalledApps}).
     * If these two version names are different then the developer has released a new version of the app.
     *
     * @param app app
     * @return is a new version of the app available
     */
    public boolean isUpdateAvailable(App app) {
        String available = getAvailableVersionOrTimestamp(app);
        String installed = getInstalledVersionOrTimestamp(packageManager, app);
        if (app == FIREFOX_LITE) {
            return !Objects.equals(available, installed.split("\\(")[0]);
        }
        return !Objects.equals(available, installed);
    }

    /**
     * If the download url is cached by {@code checkUpdateForApp} or {@code checkUpdatesForInstalledApps} then the url is returned.
     * If the download url is not cached then an empty string will be returned.
     *
     * @param app app
     * @return download url for the app or empty string
     */
    @NonNull
    public String getDownloadUrl(App app) {
        return metadataStorage.getDownloadUrl(app);
    }

    /**
     * Return the latest released version name of the app cached by {@code checkUpdateForApp} or {@code checkUpdatesForInstalledApps}.
     * If the version name is not cached, then an empty string will be returned.
     *
     * @param app app
     * @return latest version name from the developers or empty string
     */
    @NonNull
    public String getAvailableVersionOrTimestamp(App app) {
        if (app.getCompareMethodForUpdateCheck() == VERSION) {
            return metadataStorage.getVersionName(app);
        } else {
            return metadataStorage.getAvailableTimestamp(app);
        }
    }

    @NonNull
    public String getInstalledVersionOrTimestamp(PackageManager packageManager, App app) {
        if (app.getCompareMethodForUpdateCheck() == VERSION) {
            return InstalledApps.getVersionName(packageManager, app);
        } else {
            return metadataStorage.getInstalledTimestamp(app);
        }
    }

    public void setInstalledVersionOrTimestamp(App app, String versionOrTimestamp) {
        if (app.getCompareMethodForUpdateCheck() == TIMESTAMP) {
            metadataStorage.updateInstalledTimestamp(app, versionOrTimestamp);
        }
    }

    /**
     * Check for updates of installed applications and get the download links for these applications.
     *
     * @param activity if null, then the callback will be executed on a Non-Ui-Thread.
     *                 if not null, then the callback will be executed on the Ui-Thread.
     * @param callback if null, then the method will block until all network requests are finished.
     *                 if not null, then method will not block and the callback is executed in a
     *                 different thread {@see activity}.
     */
    public void checkUpdatesForInstalledApps(@Nullable Activity activity, Runnable callback) {
        checkUpdates(InstalledApps.getInstalledApps(packageManager), activity, callback);
    }

    /**
     * Check for updates of installed applications and get the download links for these applications.
     *
     * @param disabledApps these apps will be excluded from the check
     * @param activity     if null, then the callback will be executed on a Non-Ui-Thread.
     *                     if not null, then the callback will be executed on the Ui-Thread.
     * @param callback     if null, then the method will block until all network requests are finished.
     *                     if not null, then method will not block and the callback is executed in a
     *                     different thread {@see activity}.
     */
    public void checkUpdatesForInstalledApps(Set<App> disabledApps, @Nullable Activity activity, Runnable callback) {
        List<App> apps = InstalledApps.getInstalledApps(packageManager);
        apps.removeAll(disabledApps);
        checkUpdates(apps, activity, callback);
    }

    /**
     * Check for update and get the download link for a specific app.
     *
     * @param app      specific app
     * @param activity if null, then the callback will be executed on a Non-Ui-Thread.
     *                 if not null, then the callback will be executed on the Ui-Thread.
     * @param callback if null, then the method will block until all network requests are finished.
     *                 if not null, then method will not block and the callback is executed in a
     *                 different thread {@see activity}.
     */
    public void checkUpdateForApp(App app, @Nullable Activity activity, Runnable callback) {
        checkUpdates(Collections.singletonList(app), activity, callback);
    }

    private void checkUpdates(List<App> apps, @Nullable Activity activity, Runnable callback) {
        if (!futures.isEmpty()) {
            Log.w("AvailableVersions", "skip because an update is still pending");
            return;
        }

        Set<App> supportedApps = filterApps(new HashSet<>(apps));
        if (supportedApps.contains(FIREFOX_FOCUS)) {
            futures.add(executorService.submit(this::checkFocus));
        }
        if (supportedApps.contains(FIREFOX_KLAR)) {
            futures.add(executorService.submit(this::checkKlar));
        }
        if (supportedApps.contains(FIREFOX_LITE)) {
            futures.add(executorService.submit(this::checkLite));
        }
        if (supportedApps.contains(FIREFOX_RELEASE)) {
            futures.add(executorService.submit(this::checkFirefoxRelease));
        }
        if (supportedApps.contains(FIREFOX_BETA)) {
            futures.add(executorService.submit(this::checkFirefoxBeta));
        }
        if (supportedApps.contains(FIREFOX_NIGHTLY)) {
            futures.add(executorService.submit(this::checkFirefoxNightly));
        }
        if (supportedApps.contains(LOCKWISE)) {
            futures.add(executorService.submit(this::checkLockwise));
        }

        if (callback == null) {
            waitUntilAllFinished();
        } else {
            executorService.submit(() -> {
                waitUntilAllFinished();
                if (activity == null) {
                    callback.run();
                } else {
                    activity.runOnUiThread(callback);
                }
            });
        }
    }

    private void waitUntilAllFinished() {
        while (!futures.isEmpty()) {
            try {
                futures.element().get(30, TimeUnit.SECONDS);
                futures.remove();
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                futures.remove();
                Log.e("AvailableVersions", "wait too long", e);
            }
        }
    }

    /**
     * Return only apps which are:
     * - compatible with the current device
     * - in need for new metadata (because the existing metadata is too old) @see {@link MetadataCache#isTimestampTooOld(App)}
     *
     * @param apps all apps
     * @return apps matching the criteria
     */
    private Set<App> filterApps(Set<App> apps) {
        Set<App> supportedApps = new HashSet<>(apps.size());
        for (App app : apps) {
            if (app.isCompatibleWithDevice(deviceABI) && metadataStorage.isTimestampTooOld(app)) {
                supportedApps.add(app);
            }
        }
        return supportedApps;
    }

    private void checkFocus() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FOCUS);
        Focus focus = Focus.findLatest(FIREFOX_FOCUS, deviceABI.getBestSuitedAbi());
        metadataStorage.updateAvailableTimestampAndDownloadUrl(FIREFOX_FOCUS, focus.getTimestamp(), focus.getDownloadUrl());
    }

    private void checkKlar() {
        TrafficStats.setThreadStatsTag(TRAFFIC_KLAR);
        Focus klar = Focus.findLatest(FIREFOX_KLAR, deviceABI.getBestSuitedAbi());
        metadataStorage.updateAvailableTimestampAndDownloadUrl(FIREFOX_KLAR, klar.getTimestamp(), klar.getDownloadUrl());
    }

    private void checkLite() {
        TrafficStats.setThreadStatsTag(TRAFFIC_LITE);
        FirefoxLite firefoxLite = FirefoxLite.findLatest();
        if (firefoxLite != null) {
            metadataStorage.updateAvailableVersionAndDownloadUrl(FIREFOX_LITE, firefoxLite.getVersion(), firefoxLite.getDownloadUrl());
        }
    }

    private void checkFirefoxRelease() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FIREFOX_RELEASE);
        Firefox firefox = Firefox.findLatest(FIREFOX_RELEASE, deviceABI.getBestSuitedAbi());
        metadataStorage.updateAvailableTimestampAndDownloadUrl(FIREFOX_RELEASE, firefox.getTimestamp(), firefox.getDownloadUrl());
    }

    private void checkFirefoxBeta() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FIREFOX_BETA);
        Firefox firefox = Firefox.findLatest(FIREFOX_BETA, deviceABI.getBestSuitedAbi());
        metadataStorage.updateAvailableTimestampAndDownloadUrl(FIREFOX_BETA, firefox.getTimestamp(), firefox.getDownloadUrl());
    }

    private void checkFirefoxNightly() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FIREFOX_NIGHTLY);
        Firefox firefox = Firefox.findLatest(FIREFOX_NIGHTLY, deviceABI.getBestSuitedAbi());
        metadataStorage.updateAvailableTimestampAndDownloadUrl(FIREFOX_NIGHTLY, firefox.getTimestamp(), firefox.getDownloadUrl());
    }

    private void checkLockwise() {
        TrafficStats.setThreadStatsTag(TRAFFIC_LOCKWISE);
        Lockwise lockwise = Lockwise.findLatest();
        if (lockwise != null) {
            metadataStorage.updateAvailableVersionAndDownloadUrl(LOCKWISE, lockwise.getVersion(), lockwise.getDownloadUrl());
        }
    }
}