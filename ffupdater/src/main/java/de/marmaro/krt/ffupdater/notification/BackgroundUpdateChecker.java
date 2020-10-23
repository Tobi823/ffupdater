package de.marmaro.krt.ffupdater.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

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
import de.marmaro.krt.ffupdater.metadata.InstalledMetadata;
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
    private static final String LOG_TAG = "Notificator";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public BackgroundUpdateChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(LOG_TAG, "start background update check");
        final Context context = getApplicationContext();

        final List<App> appsToUpdate = findAppsWithAvailableUpdates(new SettingsHelper(context),
                new DeviceEnvironment(),
                context.getPackageManager(),
                PreferenceManager.getDefaultSharedPreferences(context));

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        new UpdateNotificationManager(context, notificationManager).showNotifications(appsToUpdate);
        return Result.success();
    }

    private List<App> findAppsWithAvailableUpdates(SettingsHelper settingsHelper, DeviceEnvironment deviceEnvironment,
                                                   PackageManager packageManager, SharedPreferences preferences) {
        final InstalledMetadataRegister register = new InstalledMetadataRegister(packageManager, preferences);
        final AvailableMetadataFetcher fetcher = new AvailableMetadataFetcher(preferences, deviceEnvironment);
        final UpdateChecker updateChecker = new UpdateChecker();

        final List<App> appsToUpdate = new ArrayList<>();
        fetcher.fetchMetadata(getAppsWhichShouldBeChecked(settingsHelper, register)).forEach((app, future) -> {
            try {
                final AvailableMetadata available = future.get(TIMEOUT.getSeconds(), TimeUnit.SECONDS);
                final InstalledMetadata installed = register.getMetadata(app).orElseThrow(NullPointerException::new);
                if (updateChecker.isUpdateAvailable(app, installed, available)) {
                    appsToUpdate.add(app);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException | ParamRuntimeException e) {
                Log.e(LOG_TAG, "update check failed for " + app, e);
            }
        });
        return appsToUpdate;
    }

    private List<App> getAppsWhichShouldBeChecked(SettingsHelper settingsHelper, InstalledMetadataRegister register) {
        final List<App> apps = register.getInstalledApps();
        apps.removeAll(settingsHelper.getDisableApps());
        return apps;
    }

}
