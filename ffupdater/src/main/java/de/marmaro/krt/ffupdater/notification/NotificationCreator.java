package de.marmaro.krt.ffupdater.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.AvailableApps;
import de.marmaro.krt.ffupdater.InstalledAppsDetector;
import de.marmaro.krt.ffupdater.MainActivity;
import de.marmaro.krt.ffupdater.R;

import static androidx.work.ExistingPeriodicWorkPolicy.REPLACE;

/**
 * This class will call the {@link WorkManager} to check regularly for app updates in the background.
 * When an app update is available, a notification will be created and displayed.
 */
public class NotificationCreator extends Worker {
    private static final String CHANNEL_ID = "update_notification_channel_id";
    private static final int REQUEST_CODE_START_MAIN_ACTIVITY = 2;
    private static final String WORK_MANAGER_KEY = "update_checker";

    private NotificationCreator(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Register NotificationCreator for regularly update checks.
     * If NotificationCreator is already registered, the already registered NotificationCreator will be replaced.
     * If pref_check_interval (from default shared preferences) is less or equal 0, NotificationCreator will be unregistered.
     *
     * @param context necessary context for accessing default shared preferences and using {@link WorkManager}.
     */
    static void register(Context context) {
        String userPref = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_check_interval), null);
        int defaultPref = context.getResources().getInteger(R.integer.default_pref_check_interval);
        int pref = NumberUtils.toInt(userPref, defaultPref);
        register(context, pref);
    }

    /**
     * Register NotificationCreator for regularly update checks.
     * If NotificationCreator is already registered, the already registered NotificationCreator will be replaced.
     * If pref_check_interval (from default shared preferences) is less or equal 0, NotificationCreator will be unregistered.
     *
     * @param context            necessary context using {@link WorkManager}.
     * @param repeatEveryMinutes check for app update every x minutes
     */
    public static void register(Context context, int repeatEveryMinutes) {
        if (repeatEveryMinutes <= 0) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY);
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(NotificationCreator.class, repeatEveryMinutes, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest);
    }

    /**
     * This method will be called by the WorkManager regularly.
     *
     * @return every method call will return a Result successfully.
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d("NotificationCreator", "doWork() executed");
        if (isUpdateAvailable()) {
            createNotification();
        }
        return Result.success();
    }

    /**
     * Check if a update for an installed app is available.
     * If an API (for example Github) is not available, the method will ignore it.
     *
     * @return an update for at least one installed app is available.
     */
    private boolean isUpdateAvailable() {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        InstalledAppsDetector detector = new InstalledAppsDetector(packageManager);
        Set<App> installedApps = new HashSet<>(detector.getInstalledApps());
        AvailableApps availableApps = AvailableApps.create(installedApps);
        for (App installedApp : installedApps) {
            String versionName = detector.getVersionName(installedApp);
            if (availableApps.isUpdateAvailable(installedApp, versionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new notification about a new app update.
     */
    private void createNotification() {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), REQUEST_CODE_START_MAIN_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = builder.setSmallIcon(R.mipmap.transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getApplicationContext().getString(R.string.update_notification_title))
                .setContentText(getApplicationContext().getString(R.string.update_notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    /**
     * This method will create a notification channel for the "update notification".
     * Reason: Since Android 9 notification can only be created with an existing notification channel.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        CharSequence channelName = getApplicationContext().getString(R.string.update_notification_channel_name);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(getApplicationContext().getString(R.string.update_notification_channel_description));

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}
