package de.marmaro.krt.ffupdater.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
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
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.MainActivity;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadataFetcher;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadata;
import de.marmaro.krt.ffupdater.metadata.InstalledMetadataRegister;
import de.marmaro.krt.ffupdater.metadata.UpdateChecker;
import de.marmaro.krt.ffupdater.settings.SettingsHelper;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.work.ExistingPeriodicWorkPolicy.REPLACE;
import static de.marmaro.krt.ffupdater.R.mipmap.ic_launcher;
import static de.marmaro.krt.ffupdater.R.mipmap.transparent;
import static de.marmaro.krt.ffupdater.R.string.notification_text;
import static de.marmaro.krt.ffupdater.R.string.notification_title;
import static de.marmaro.krt.ffupdater.R.string.update_notification_channel_description;
import static de.marmaro.krt.ffupdater.R.string.update_notification_channel_name;

/**
 * This class will call the {@link WorkManager} to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 */
public class Notificator extends Worker {
    private static final String LOG_TAG = "Notificator";
    private static final String CHANNEL_ID = "update_notification_channel_id";
    private static final String WORK_MANAGER_KEY = "update_checker";
    private static final int NOTIFICATION_ID = 1;
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
        final UpdateChecker updateChecker = new UpdateChecker();

        final InstalledMetadataRegister register = new InstalledMetadataRegister(packageManager, preferences);
        List<App> apps = register.getInstalledApps();
        apps.removeAll(SettingsHelper.getDisableApps(context));

        final AvailableMetadataFetcher fetcher = new AvailableMetadataFetcher(preferences, new DeviceEnvironment());
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

        if (appsToUpdate.isEmpty()) {
            getNotificationManager().cancel(NOTIFICATION_ID);
        } else {
            createNotification(appsToUpdate);
        }
        return Result.success();
    }

    private void createNotification(List<App> apps) {
        final Context context = getApplicationContext();
        final NotificationManager notificationManager = getNotificationManager();

        final NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, notificationManager);
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(context);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_START_MAIN_ACTIVITY, intent, FLAG_UPDATE_CURRENT);
        String appsToUpdate = apps.stream().map(app -> app.getTitle(context)).collect(Collectors.joining(", "));

        Notification notification = builder.setSmallIcon(transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), ic_launcher))
                .setContentTitle(context.getString(notification_title, appsToUpdate))
                .setContentText(context.getString(notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * This method will create a notification channel for the "update notification".
     * Reason: Since API Level 28/Oreo notification can only be created with an existing notification channel.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        final NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(update_notification_channel_name),
                IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(update_notification_channel_description));
        notificationManager.createNotificationChannel(channel);
    }

    @NonNull
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }
}
