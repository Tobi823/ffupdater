package de.marmaro.krt.ffupdater.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import static androidx.work.ExistingPeriodicWorkPolicy.REPLACE;

/**
 * This class will call the {@link WorkManager} to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 */
public class Notificator extends Worker {
    private static final String LOG_TAG = "Notificator";
    private static final String WORK_MANAGER_KEY = "update_checker";
    private static final int REQUEST_CODE_START_MAIN_ACTIVITY = 2;

    public Notificator(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * You can
     * - start a new
     * - replace the current running
     * - or stop the current running
     * background update check (depending on the settings by the user). Only one background job at a time can exists.
     *
     * @param context context
     */
    public static void start(Context context) {
        start(context, SettingsHelper.isAutomaticCheck(context), SettingsHelper.getCheckInterval(context));
    }

    private static void start(Context context, boolean automaticCheckInBackground, int repeatEveryMinutes) {
        if (!automaticCheckInBackground) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY);
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(Notificator.class, repeatEveryMinutes, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest);
    }

    /**
     * This method will be called by the WorkManager regularly.
     *
     * @return always return success
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d(LOG_TAG, "start background update check");
        final Context context = getApplicationContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final PackageManager packageManager = context.getPackageManager();
        final InstalledMetadataRegister register = new InstalledMetadataRegister(packageManager, preferences);
        final AvailableMetadataFetcher fetcher = new AvailableMetadataFetcher(preferences, new DeviceEnvironment());
        final UpdateChecker updateChecker = new UpdateChecker();
        final NotificationManager notificationManager = getNotificationManager();
        final SummarizedNotificationManager summarized = new SummarizedNotificationManager(context, notificationManager);
        final IndividualNotificationManager individual = new IndividualNotificationManager(context, notificationManager);

        List<App> apps = register.getInstalledApps();
        apps.removeAll(SettingsHelper.getDisableApps(context));
        final Map<App, Future<AvailableMetadata>> futures = fetcher.fetchMetadata(apps);

        final List<App> appsToUpdate = new ArrayList<>();
        futures.forEach((app, future) -> {
            try {
                AvailableMetadata available = future.get(30, TimeUnit.SECONDS);
                InstalledMetadata installed = register.getMetadata(app)
                        .orElseThrow(() -> new ParamRuntimeException("installed metadata is missing"));
                if (updateChecker.isUpdateAvailable(app, installed, available)) {
                    appsToUpdate.add(app);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException | ParamRuntimeException e) {
                Log.e(LOG_TAG, "update check failed for " + app, e);
            }
        });

        if (SettingsHelper.isSummarizeUpdateNotifications(context)) {
            summarized.showNotification(appsToUpdate);
            individual.hideNotifications();
        } else {
            individual.showNotifications(appsToUpdate);
            summarized.hideNotification();
        }
        return Result.success();
    }

    @NonNull
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }
}
