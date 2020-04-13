package de.marmaro.krt.ffupdater;

import android.content.pm.PackageManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private ExecutorService executorService;

    private Runnable callback;
    private InstalledApps installedApps;
    private List<Future> futures = new ArrayList<>();
    public Map<App, String> versions = new ConcurrentHashMap<>();
    public Map<App, String> downloadUrls = new ConcurrentHashMap<>();

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

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

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
        if (availableVersion == null || installedVersion == null) {
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

    public void checkUpdatesForInstalledApps() {
        Objects.requireNonNull(installedApps);
        checkUpdates(installedApps.getInstalledApps());
    }

    public void checkUpdatesForAllApps() {
        checkUpdates(Arrays.asList(App.values()));
    }

    public void checkUpdateForApp(App app) {
        checkUpdates(Collections.singletonList(app));
    }

    @NotNull
    public String getDownloadUrl(App app) {
        String downloadUrl = downloadUrls.get(app);
        if (downloadUrl == null) {
            return "";
        }
        return downloadUrl;
    }

    private void checkUpdates(List<App> apps) {
        Objects.requireNonNull(executorService);
        Objects.requireNonNull(callback);

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
            for (Future future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace(); //TODO
                }
            }
            callback.run();
        });
    }

    private void checkFennec(List<App> appsToCheck) {
        FennecVersionFinder.Version version = FennecVersionFinder.getVersion();
        if (version == null) {
            System.out.println("version is null");
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
        FirefoxLiteVersionFinder firefoxLiteVersionFinder = FirefoxLiteVersionFinder.create();
        if (!firefoxLiteVersionFinder.isCorrect()) {
            return;
        }

        versions.put(FIREFOX_LITE, firefoxLiteVersionFinder.getVersion());
        downloadUrls.put(FIREFOX_LITE, firefoxLiteVersionFinder.getDownloadUrl());
    }

    private void checkFenix() {
        FenixVersionFinder fenixVersionFinder = FenixVersionFinder.create();
        if (!fenixVersionFinder.isCorrect()) {
            return;
        }

        versions.put(FENIX, fenixVersionFinder.getVersion());
        downloadUrls.put(FENIX, fenixVersionFinder.getDownloadUrl(DeviceABI.getAbi()));
    }
}
