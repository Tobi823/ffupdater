package de.marmaro.krt.ffupdater.version;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import de.marmaro.krt.ffupdater.utils.Utils;

import static de.marmaro.krt.ffupdater.App.FENIX;
import static de.marmaro.krt.ffupdater.App.FENNEC_BETA;
import static de.marmaro.krt.ffupdater.App.FENNEC_NIGHTLY;
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

    private ExecutorService executorService;
    private PackageManager packageManager;
    private Queue<Future> futures = new ConcurrentLinkedQueue<>();
    private Map<App, String> versions = new ConcurrentHashMap<>();
    private Map<App, String> urls = new ConcurrentHashMap<>();

    public AvailableVersions(PackageManager packageManager) {
        this.executorService = Executors.newFixedThreadPool(NUMBER_BACKGROUND_THREADS);
        this.packageManager = Objects.requireNonNull(packageManager);
    }

    /**
     * Call this method for destructing AvailableVersions correctly to avoid memory leaks.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Check with {@code isUpdateAvailable} if any installed app is outdated.
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
        if (!versions.containsKey(app)) {
            return false;
        }
        String available = Objects.requireNonNull(versions.get(app));
        String installed = InstalledApps.getVersionName(packageManager, app);

        if (app == FENNEC_BETA) {
            String sanitizedAvailable = available.split("b")[0];
            return !sanitizedAvailable.contentEquals(installed);
        }
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
    @NotNull
    public String getDownloadUrl(App app) {
        return Utils.convertNullToEmptyString(urls.get(app));
    }

    /**
     * Return the latest released version name of the app cached by {@code checkUpdateForApp} or {@code checkUpdatesForInstalledApps}.
     * If the version name is not cached, then an empty string will be returned.
     *
     * @param app app
     * @return latest version name from the developers or empty string
     */
    @NotNull
    public String getAvailableVersion(App app) {
        return Utils.convertNullToEmptyString(versions.get(app));
    }

    /**
     * Check for updates of installed applications and get the download links for these applications.
     *
     * @param activity if null, then the callback will be executed on a Non-Ui-Thread
     *                 if not null, then the callback will be executed on the Ui-Thread
     * @param callback will be executed, when the check is ready
     */
    public void checkUpdatesForInstalledApps(@Nullable Activity activity, Runnable callback) {
        checkUpdates(InstalledApps.getInstalledApps(packageManager), activity, callback);
    }

    /**
     * Check for updates of installed applications and get the download links for these applications.
     *
     * @param disabledApps these apps will be excluded from the check
     * @param activity     if null, then the callback will be executed on a Non-Ui-Thread
     *                     if not null, then the callback will be executed on the Ui-Thread
     * @param callback     will be executed, when the check is ready
     */
    public void checkUpdatesForInstalledApps(Set<App> disabledApps, @Nullable Activity activity, Runnable callback) {
        List<App> apps = InstalledApps.getInstalledApps(packageManager);
        apps.removeAll(disabledApps);
        checkUpdates(apps, activity, callback);
    }

    /**
     * Check for update and get the download link for a specific app.
     *
     * @param app specific app
     * @param activity if null, then the callback will be executed on a Non-Ui-Thread
     *                 if not null, then the callback will be executed on the Ui-Thread
     * @param callback will be executed, when the check is ready
     */
    public void checkUpdateForApp(App app, @Nullable Activity activity, Runnable callback) {
        checkUpdates(Collections.singletonList(app), activity, callback);
    }

    private void checkUpdates(List<App> apps, @Nullable Activity activity, Runnable callback) {
        if (!futures.isEmpty()) {
            Log.w("AvailableVersions", "skip because an update is still pending");
            return;
        }

        List<App> supportedApps = filterUnsupportedApps(apps);
        if (!Collections.disjoint(supportedApps, Arrays.asList(FENNEC_RELEASE, FENNEC_BETA, FENNEC_NIGHTLY))) {
            futures.add(executorService.submit(() -> checkFennec(supportedApps)));
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

        executorService.submit(() -> {
            while (!futures.isEmpty()) {
                try {
                    futures.element().get(10, TimeUnit.SECONDS);
                    futures.remove();
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    Log.e("AvailableVersions", "wait too long", e);
                }
            }
            if (activity == null) {
                callback.run();
            } else {
                activity.runOnUiThread(callback);
            }
        });
    }

    private List<App> filterUnsupportedApps(List<App> apps) {
        List<App> supportedApps = new ArrayList<>(apps.size());
        for (App app : apps) {
            if (app.isCompatibleWithDevice()) {
                supportedApps.add(app);
            }
        }
        return supportedApps;
    }

    private void checkFennec(List<App> appsToCheck) {
        TrafficStats.setThreadStatsTag(TRAFFIC_FENNEC);
        Fennec fennec = Fennec.findLatest();
        if (fennec == null) {
            return;
        }

        for (App app : Arrays.asList(FENNEC_RELEASE, FENNEC_BETA, FENNEC_NIGHTLY)) {
            if (appsToCheck.contains(app)) {
                versions.put(app, fennec.getVersion(app));
                urls.put(app, fennec.getDownloadUrl(app, DeviceABI.getBestSuitedAbi()));
            }
        }
    }

    private void checkFocusKlar(List<App> appsToCheck) {
        TrafficStats.setThreadStatsTag(TRAFFIC_FOCUS);
        Focus focus = Focus.findLatest();
        if (focus == null) {
            return;
        }

        for (App app : Arrays.asList(FIREFOX_FOCUS, FIREFOX_KLAR)) {
            if (appsToCheck.contains(app)) {
                versions.put(app, focus.getVersion());
                urls.put(app, focus.getDownloadUrl(app, DeviceABI.getBestSuitedAbi()));
            }
        }
    }

    private void checkLite() {
        TrafficStats.setThreadStatsTag(TRAFFIC_LITE);
        FirefoxLite firefoxLite = FirefoxLite.findLatest();
        if (firefoxLite == null) {
            return;
        }

        versions.put(FIREFOX_LITE, firefoxLite.getVersion());
        urls.put(FIREFOX_LITE, firefoxLite.getDownloadUrl());
    }

    private void checkFenix() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FENIX);
        Fenix fenix = Fenix.findLatest();
        if (fenix == null) {
            return;
        }

        versions.put(FENIX, fenix.getVersion());
        urls.put(FENIX, fenix.getDownloadUrl(DeviceABI.getBestSuitedAbi()));
    }
}