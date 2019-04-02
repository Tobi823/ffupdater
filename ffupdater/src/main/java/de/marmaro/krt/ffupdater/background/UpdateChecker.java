package de.marmaro.krt.ffupdater.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.marmaro.krt.ffupdater.FirefoxMetadata;
import de.marmaro.krt.ffupdater.MainActivity;
import de.marmaro.krt.ffupdater.MozillaVersions;
import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.Version;

import static androidx.work.ExistingPeriodicWorkPolicy.KEEP;
import static androidx.work.ExistingPeriodicWorkPolicy.REPLACE;

/**
 * Created by Tobiwan on 01.04.2019.
 */
public class UpdateChecker extends Worker {
    private static final String CHANNEL_ID = "update_available2";
    public static final String UPDATE_AVAILABLE_RESPONSE = "update_available";
    public static final String WORK_MANAGER_KEY = "update_checker";
    public static final String CREATION_DATE_TIME = "creation_date_time";
    public static final String FIRST_EXECUTION_DATE_TIME = "first_execution_date_time";
    public static final String WITH_INITIAL_DELAY = "with_initial_delay";
    public static final String NOT_EXECUTED = "not_executed";

    public static final int REQUEST_CODE_START_MAIN_ACTIVITY = 2;


    public UpdateChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void register() {
        registerWithInitialDelay(0);
    }

    public static void registerWithInitialDelay(long delayInMs) {
        long currentTimeInMillis = System.currentTimeMillis();
        Data startTime = new Data.Builder()
                .putBoolean(WITH_INITIAL_DELAY, delayInMs != 0)
                .putLong(CREATION_DATE_TIME, currentTimeInMillis)
                .putLong(FIRST_EXECUTION_DATE_TIME, currentTimeInMillis + delayInMs)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(UpdateChecker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .setInputData(startTime)
                        .build();

        WorkManager.getInstance().enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (shouldWorkBeDelayed()) {
            return Result.success(new Data.Builder().putBoolean(NOT_EXECUTED, true).build());
        }
        boolean updateAvailable = isUpdateAvailable();
        if (updateAvailable) {
            showNotification();
        }
        return Result.success(new Data.Builder().putBoolean(UPDATE_AVAILABLE_RESPONSE, updateAvailable).build());
    }

    private boolean shouldWorkBeDelayed() {
        if (getInputData().getBoolean(WITH_INITIAL_DELAY, false)) {
            return false;
        }

        long currentTimeInMillis = System.currentTimeMillis();
        long createdDateTime = getInputData().getLong(CREATION_DATE_TIME, 0);
        long firstExecutionDateTime = getInputData().getLong(FIRST_EXECUTION_DATE_TIME, 0);
        return createdDateTime <= currentTimeInMillis && currentTimeInMillis < firstExecutionDateTime;
    }

    private boolean isUpdateAvailable() {
        Version current = new FirefoxMetadata.Builder().checkLocalInstalledFirefox(getApplicationContext().getPackageManager()).getVersion();
        Version latest = MozillaVersions.getVersion();
        return (current.compareTo(latest) < 0);
    }

    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Update Notification";
            String description = "Channel for new available updates";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        } else {
            //noinspection deprecation
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        // access the big app icon as bitmap
        Bitmap largeIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);

        // open main view when notification was pressed
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.OPENED_BY_NOTIFICATION, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), REQUEST_CODE_START_MAIN_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = builder.setSmallIcon(R.mipmap.transparent, 0)
                .setSmallIcon(R.mipmap.transparent, 0)
                .setLargeIcon(largeIcon)
                .setContentTitle(getApplicationContext().getString(R.string.update_notification_title))
                .setContentText(getApplicationContext().getString(R.string.update_notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1, notification);
    }


}
