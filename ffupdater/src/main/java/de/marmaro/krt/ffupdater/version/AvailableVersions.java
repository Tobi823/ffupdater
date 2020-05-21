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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import de.marmaro.krt.ffupdater.device.DeviceABI;
import de.marmaro.krt.ffupdater.device.InstalledApps;

import static de.marmaro.krt.ffupdater.App.FENIX;
import static de.marmaro.krt.ffupdater.App.FENNEC_RELEASE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;

/**
 * Helps to fetch the latest version name of apps with background threads.
 */
public class AvailableVersions {
    // StrictMode needs for every running thread a stats id
    private static final int TRAFFIC_FENNEC = 1001;
    private static final int TRAFFIC_FOCUS = 1002;
    private static final int TRAFFIC_LITE = 1003;
    private static final int TRAFFIC_FENIX = 1004;
    private static final int NUMBER_BACKGROUND_THREADS = 5;

    private final ExecutorService executorService;
    private final PackageManager packageManager;
    private final Queue<Future> futures = new ConcurrentLinkedQueue<>();
    private final MetadataCache metadataStorage;
    private final DeviceABI deviceABI;

    public AvailableVersions(Context context) {
        this(Preconditions.checkNotNull(
                context.getPackageManager()),
                PreferenceManager.getDefaultSharedPreferences(context),
                new DeviceABI());
    }

    public AvailableVersions(PackageManager packageManager, SharedPreferences sharedPreferences, DeviceABI deviceABI) {
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
        String available = metadataStorage.getVersionName(app);
        String installed = InstalledApps.getVersionName(packageManager, app);

        if (app == FIREFOX_LITE) {
            String sanitizedInstalled = installed.split("\\(")[0];
            return !available.contentEquals(sanitizedInstalled);
        }
        return !available.contentEquals(installed);
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
    public String getAvailableVersion(App app) {
        return metadataStorage.getVersionName(app);
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
        if (supportedApps.contains(FENNEC_RELEASE)) {
            futures.add(executorService.submit(this::checkFennec));
        }
        if (!Collections.disjoint(supportedApps, Arrays.asList(FIREFOX_KLAR, FIREFOX_FOCUS))) {
            futures.add(executorService.submit(() -> checkFocusKlar(supportedApps)));
        }
        if (supportedApps.contains(FIREFOX_LITE)) {
            futures.add(executorService.submit(this::checkLite));
        }
        if (supportedApps.contains(FENIX)) {
            futures.add(executorService.submit(this::checkFenix));
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
                futures.element().get(10, TimeUnit.SECONDS);
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

    private void checkFennec() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FENNEC);
        Fennec fennec = Fennec.findLatest();
        if (fennec != null) {
            metadataStorage.setMetadata(FENNEC_RELEASE, fennec.getVersion(), fennec.getDownloadUrl(deviceABI.getBestSuitedAbi()));
        }
    }

    private void checkFocusKlar(Set<App> appsToCheck) {
        TrafficStats.setThreadStatsTag(TRAFFIC_FOCUS);
        Focus focus = Focus.findLatest();
        if (focus == null) {
            return;
        }

        for (App app : Arrays.asList(FIREFOX_FOCUS, FIREFOX_KLAR)) {
            if (appsToCheck.contains(app)) {
                metadataStorage.setMetadata(app, focus.getVersion(), focus.getDownloadUrl(app, deviceABI.getBestSuitedAbi()));
            }
        }
    }

    private void checkLite() {
        TrafficStats.setThreadStatsTag(TRAFFIC_LITE);
        FirefoxLite firefoxLite = FirefoxLite.findLatest();
        if (firefoxLite != null) {
            metadataStorage.setMetadata(FIREFOX_LITE, firefoxLite.getVersion(), firefoxLite.getDownloadUrl());
        }
    }

    private void checkFenix() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FENIX);
        Fenix fenix = Fenix.findLatest();
        if (fenix != null) {
            metadataStorage.setMetadata(FENIX, fenix.getVersion(), fenix.getDownloadUrl(deviceABI.getBestSuitedAbi()));
        }
    }
}