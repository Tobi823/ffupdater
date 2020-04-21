package de.marmaro.krt.ffupdater;

import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.device.DeviceABI;
import de.marmaro.krt.ffupdater.device.InstalledApps;
import de.marmaro.krt.ffupdater.download.FenixVersionFinder;
import de.marmaro.krt.ffupdater.download.FennecVersionFinder;
import de.marmaro.krt.ffupdater.download.FirefoxLiteVersionFinder;
import de.marmaro.krt.ffupdater.download.FocusVersionFinder;

import static de.marmaro.krt.ffupdater.App.FENIX;
import static de.marmaro.krt.ffupdater.App.FENNEC_BETA;
import static de.marmaro.krt.ffupdater.App.FENNEC_NIGHTLY;
import static de.marmaro.krt.ffupdater.App.FENNEC_RELEASE;
import static de.marmaro.krt.ffupdater.App.FIREFOX_FOCUS;
import static de.marmaro.krt.ffupdater.App.FIREFOX_KLAR;
import static de.marmaro.krt.ffupdater.App.FIREFOX_LITE;

/**
 * Created by Tobiwan on 13.04.2020.
 */
public class AppUpdate {
    private static final int TRAFFIC_FENNEC = 1001;
    private static final int TRAFFIC_FOCUS = 1002;
    private static final int TRAFFIC_LITE = 1003;
    private static final int TRAFFIC_FENIX = 1004;
    private ExecutorService executorService;

    private InstalledApps installedApps;
    private Queue<Future> futures = new ConcurrentLinkedQueue<>();
    private Map<App, String> versions = new ConcurrentHashMap<>();
    private Map<App, String> downloadUrls = new ConcurrentHashMap<>();

    public static AppUpdate updateCheck(PackageManager packageManager) {
        return new AppUpdate(
                Executors.newFixedThreadPool(5),
                new InstalledApps(packageManager));
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public AppUpdate(ExecutorService executorService, InstalledApps installedApps) {
        Objects.requireNonNull(executorService);
        Objects.requireNonNull(installedApps);
        this.executorService = executorService;
        this.installedApps = installedApps;
    }

    public boolean isAppInstalled(App app) {
        return installedApps.isInstalled(app);
    }

    // TODO rename
    public boolean isUpdateAvailable() {
        for (App app : installedApps.getInstalledApps()) {
            if (isUpdateAvailable(app)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUpdateAvailable(App app) {
        String availableVersion = versions.get(app);
        String installedVersion = installedApps.getVersionName(app);
        if (availableVersion == null) {
            return false; //TODO installedVersion should be notnull
        }

        if (app == FENNEC_BETA) {
            String sanitizedAvailable = availableVersion.split("b")[0];
            return !sanitizedAvailable.contentEquals(installedVersion);
        }
        if (app == FIREFOX_LITE) {
            String sanitizedInstalled = installedVersion.split("\\(")[0];
            return !availableVersion.contentEquals(sanitizedInstalled);
        }
        return !availableVersion.contentEquals(installedVersion);
    }

    public void checkUpdatesForInstalledApps(Runnable callback) {
        checkUpdates(installedApps.getInstalledApps(), callback);
    }

    public void checkUpdateForApp(App app, Runnable callback) {
        checkUpdates(Collections.singletonList(app), callback);
    }

    @NotNull
    public String getDownloadUrl(App app) {
        String downloadUrl = downloadUrls.get(app);
        if (downloadUrl == null) {
            return "";
        }
        return downloadUrl;
    }

    public boolean isDownloadUrlCached(App app) {
        return !getDownloadUrl(app).isEmpty();
    }

    @NotNull
    public String getInstalledVersion(App app) {
        return installedApps.getVersionName(app);
    }

    @NotNull
    public String getAvailableVersion(App app) {
        String version = versions.get(app);
        if (version == null) {
            return "";
        }
        return version;
    }

    private void checkUpdates(List<App> apps, Runnable callback) {
        Objects.requireNonNull(executorService);
        if (!futures.isEmpty()) {
            Log.w("AppUpdate", "skip because an update is still pending");
            return;
        }

        if (apps.contains(FENNEC_RELEASE) ||
                apps.contains(FENNEC_BETA) ||
                apps.contains(FENNEC_NIGHTLY)) {
            futures.add(executorService.submit(() -> checkFennec(apps)));
        }
        if (apps.contains(FIREFOX_KLAR) ||
                apps.contains(FIREFOX_FOCUS)) {
            futures.add(executorService.submit(() -> checkFocusKlar(apps)));
        }
        if (apps.contains(FIREFOX_LITE)) {
            futures.add(executorService.submit(this::checkLite));
        }
        if (apps.contains(FENIX)) {
            futures.add(executorService.submit(this::checkFenix));
        }

        executorService.submit(() -> {
            while (!futures.isEmpty()) {
                try {
                    futures.element().get(10, TimeUnit.SECONDS);
                    futures.remove();
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    Log.e("AppUpdate", "wait too long", e);
                }
            }
            callback.run();
        });
    }

    private void checkFennec(List<App> appsToCheck) {
        TrafficStats.setThreadStatsTag(TRAFFIC_FENNEC);
        FennecVersionFinder.Version version = FennecVersionFinder.getVersion();
        if (version == null) {
            return;
        }

        if (appsToCheck.contains(FENNEC_RELEASE)) {
            versions.put(FENNEC_RELEASE, version.getReleaseVersion());
            downloadUrls.put(FENNEC_RELEASE, FennecVersionFinder.getDownloadUrl(FENNEC_RELEASE, DeviceABI.getAbi()));
        }
        if (appsToCheck.contains(FENNEC_BETA)) {
            versions.put(FENNEC_BETA, version.getBetaVersion());
            downloadUrls.put(FENNEC_BETA, FennecVersionFinder.getDownloadUrl(FENNEC_BETA, DeviceABI.getAbi()));
        }
        if (appsToCheck.contains(FENNEC_NIGHTLY)) {
            versions.put(FENNEC_NIGHTLY, version.getNightlyVersion());
            downloadUrls.put(FENNEC_NIGHTLY, FennecVersionFinder.getDownloadUrl(FENNEC_NIGHTLY, DeviceABI.getAbi()));
        }
    }

    private void checkFocusKlar(List<App> appsToCheck) {
        TrafficStats.setThreadStatsTag(TRAFFIC_FOCUS);
        FocusVersionFinder focusVersionFinder = FocusVersionFinder.create();
        if (!focusVersionFinder.isCorrect()) {
            return;
        }

        if (appsToCheck.contains(FIREFOX_FOCUS)) {
            versions.put(FIREFOX_FOCUS, focusVersionFinder.getVersion());
            downloadUrls.put(FIREFOX_FOCUS, focusVersionFinder.getDownloadUrl(FIREFOX_FOCUS, DeviceABI.getAbi()));
        }
        if (appsToCheck.contains(FIREFOX_KLAR)) {
            versions.put(FIREFOX_KLAR, focusVersionFinder.getVersion());
            downloadUrls.put(FIREFOX_KLAR, focusVersionFinder.getDownloadUrl(FIREFOX_KLAR, DeviceABI.getAbi()));
        }
    }

    private void checkLite() {
        TrafficStats.setThreadStatsTag(TRAFFIC_LITE);
        FirefoxLiteVersionFinder firefoxLiteVersionFinder = FirefoxLiteVersionFinder.create();
        if (!firefoxLiteVersionFinder.isCorrect()) {
            return;
        }

        versions.put(FIREFOX_LITE, firefoxLiteVersionFinder.getVersion());
        downloadUrls.put(FIREFOX_LITE, firefoxLiteVersionFinder.getDownloadUrl());
    }

    private void checkFenix() {
        TrafficStats.setThreadStatsTag(TRAFFIC_FENIX);
        FenixVersionFinder fenixVersionFinder = FenixVersionFinder.create();
        if (!fenixVersionFinder.isCorrect()) {
            return;
        }

        versions.put(FENIX, fenixVersionFinder.getVersion());
        downloadUrls.put(FENIX, fenixVersionFinder.getDownloadUrl(DeviceABI.getAbi()));
    }
}
