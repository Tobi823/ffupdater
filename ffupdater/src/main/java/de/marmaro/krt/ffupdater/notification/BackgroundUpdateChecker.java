package de.marmaro.krt.ffupdater.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadataFetcher;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadataRegister;
import de.marmaro.krt.ffupdater.metadata.UpdateChecker;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * This class will call the {@link WorkManager} to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 */
public class BackgroundUpdateChecker extends Worker {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public BackgroundUpdateChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            doBackgroundCheck();
            return Result.success();
        } catch (RuntimeException exception) {
            showErrorNotification(exception);
            return Result.failure();
        }
    }

    private void doBackgroundCheck() {
        final Context context = getApplicationContext();
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        final SettingsHelper settings = new SettingsHelper(context);
        final DeviceEnvironment environment = new DeviceEnvironment();
        final PackageManager packageManager = context.getPackageManager();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final List<App> apps = findAppsWithAvailableUpdates(settings, environment, packageManager, preferences);
        new UpdateNotificationManager(context, notificationManager).showNotifications(apps);
    }

    private List<App> findAppsWithAvailableUpdates(SettingsHelper settingsHelper,
                                                   DeviceEnvironment deviceEnvironment,
                                                   PackageManager packageManager,
                                                   SharedPreferences preferences) {
        final InstalledMetadataRegister register = new InstalledMetadataRegister(packageManager, preferences);
        final AvailableMetadataFetcher fetcher = new AvailableMetadataFetcher(preferences, deviceEnvironment);
        final UpdateChecker updateChecker = new UpdateChecker();

        final List<App> appsToUpdate = new ArrayList<>();
        fetcher.fetchMetadata(getAppsWhichShouldBeChecked(settingsHelper, register)).forEach((app, future) -> {
            try {
                final AvailableMetadata available = future.get(TIMEOUT.getSeconds(), TimeUnit.SECONDS);
                register.getMetadata(app).ifPresent(installed -> {
                    if (updateChecker.isUpdateAvailable(app, installed, available)) {
                        appsToUpdate.add(app);
                    }
                });
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new ParamRuntimeException(e, "background update check failed for %s", app);
            }
        });
        return appsToUpdate;
    }

    private List<App> getAppsWhichShouldBeChecked(SettingsHelper settingsHelper, InstalledMetadataRegister register) {
        final List<App> apps = register.getInstalledApps();
        apps.removeAll(settingsHelper.getDisableApps());
        return apps;
    }

    private void showErrorNotification(Exception exception) {
        final Context context = getApplicationContext();
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        new ErrorNotificationManager(context, notificationManager).showNotification(exception);
    }
}
