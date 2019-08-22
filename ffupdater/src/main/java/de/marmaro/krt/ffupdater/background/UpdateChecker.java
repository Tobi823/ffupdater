package de.marmaro.krt.ffupdater.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import de.marmaro.krt.ffupdater.MainActivity;
import de.marmaro.krt.ffupdater.R;

import static androidx.work.ExistingPeriodicWorkPolicy.REPLACE;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by Tobiwan on 01.04.2019.
 */
public class UpdateChecker extends Worker {
    private static final String CHANNEL_ID = "update_notification_channel_id";
    public static final String UPDATE_AVAILABLE_RESPONSE = "update_available";
    public static final String WORK_MANAGER_KEY = "update_checker";

    public static final int REQUEST_CODE_START_MAIN_ACTIVITY = 2;

    public UpdateChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * (Re)register or unregister UpdateChecker depending on the value of pref_check_interval.
     * If pref_check_interval is greater than 0, than UpdateChecker will be regularly executed without an initial delay.
     *
     * @param context necessary context for accessing shared preferences etc.
     */
    public static void registerOrUnregister(Context context) {
        int defaultValue = context.getResources().getInteger(R.integer.default_pref_check_interval);
        String valueAsString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_check_interval), String.valueOf(defaultValue));
        // the ListPreference only accept string-arrays as app:entryValues.
        // That's the reason why I have to parse the result string back to an int although the value
        // of the ListPreference is always only a number in a string.
        int value = Integer.parseInt(valueAsString);
        registerOrUnregister(value);
    }

    /**
     * (Re)register or unregister UpdateChecker depending on the value of pref_check_interval.
     * If pref_check_interval is greater than 0, than UpdateChecker will be regularly executed without an initial delay.
     *
     * @param repeatIntervalLengthInMinutes value of pref_check_interval
     */
    public static void registerOrUnregister(int repeatIntervalLengthInMinutes) {
        if (repeatIntervalLengthInMinutes > 0) {
            UpdateChecker.register(repeatIntervalLengthInMinutes);
        } else {
            UpdateChecker.unregister();
        }
    }

    /**
     * Register UpdateChecker which will be executed regularly.
     *
     * @param repeatIntervalLengthInMinutes time between each execution in minutes
     */
    private static void register(int repeatIntervalLengthInMinutes) {
        checkArgument(repeatIntervalLengthInMinutes >= 0, "repeatIntervalLengthInMinutes must not be negative");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(UpdateChecker.class, repeatIntervalLengthInMinutes, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance().enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest);
    }

    /**
     * Unregister UpdateChecker with the result that UpdateChecker will not be executed anymore.
     */
    private static void unregister() {
        WorkManager.getInstance().cancelUniqueWork(WORK_MANAGER_KEY);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("UpdateChecker", "doWork() executed");
        boolean updateAvailable = isUpdateAvailable();
        if (updateAvailable) {
            showNotification();
        }
        return Result.success(new Data.Builder().putBoolean(UPDATE_AVAILABLE_RESPONSE, updateAvailable).build());
    }

    private boolean isUpdateAvailable() {
//        Version current = FirefoxMetadata.create(getApplicationContext().getPackageManager()).getVersion();
//        Version latest = MozillaVersions.getVersion();
//        return !current.equals(latest);
        // TODO
        return false;
    }

    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

        notificationManager.notify(1, notification);
    }

    /**
     * This method must be called for displaying a notification for Android 9
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence name = getApplicationContext().getString(R.string.update_notification_channel_name);
        String description = getApplicationContext().getString(R.string.update_notification_channel_description);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }
}
