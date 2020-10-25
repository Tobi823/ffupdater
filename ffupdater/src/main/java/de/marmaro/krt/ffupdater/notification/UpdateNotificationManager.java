package de.marmaro.krt.ffupdater.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.InstallActivity;
import de.marmaro.krt.ffupdater.R;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static de.marmaro.krt.ffupdater.R.mipmap.transparent;

class UpdateNotificationManager {
    public static final String CHANNEL_ID_BASE = "update_notification_channel__";
    public static final int NOTIFICATION_ID_BASE = 200;

    private final Map<App, String> channelIds = new HashMap<>();
    private final Map<App, Integer> notificationIds = new HashMap<>();

    private final Context context;
    private final NotificationManager notificationManager;

    UpdateNotificationManager(Context context, NotificationManager notificationManager) {
        this.context = Objects.requireNonNull(context);
        this.notificationManager = Objects.requireNonNull(notificationManager);
        for (App app : App.values()) {
            channelIds.put(app, CHANNEL_ID_BASE + app.name().toLowerCase());
            notificationIds.put(app, NOTIFICATION_ID_BASE + app.ordinal());
        }
    }

    private void hideNotification(App app) {
        final Integer id = Objects.requireNonNull(notificationIds.get(app));
        notificationManager.cancel(id);
    }

    void showNotifications(List<App> apps) {
        for (App app : App.values()) {
            if (apps.contains(app)) {
                showNotification(app);
            } else {
                hideNotification(app);
            }
        }
    }

    private void showNotification(App app) {
        final Intent intent = new Intent(context, InstallActivity.class);
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name());
        final PendingIntent updateAppIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT);

        final String appTitle = app.getTitle(context);
        final Notification notification = createNotificationBuilder(channelIds.get(app), appTitle)
                .setSmallIcon(transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.update_notification_title, appTitle))
                .setContentText(context.getString(R.string.update_notification_text, appTitle))
                .setContentIntent(updateAppIntent)
                .setAutoCancel(true)
                .build();

        final Integer notificationId = Objects.requireNonNull(notificationIds.get(app));
        notificationManager.notify(notificationId, notification);
    }

    private NotificationCompat.Builder createNotificationBuilder(String channelId, String appTitle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId, appTitle);
            return new NotificationCompat.Builder(context, channelId);
        } else {
            //noinspection deprecation
            return new NotificationCompat.Builder(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String appTitle) {
        final NotificationChannel channel = new NotificationChannel(
                channelId,
                context.getString(R.string.update_notification_channel_name, appTitle),
                IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.update_notification_channel_description, appTitle));
        notificationManager.createNotificationChannel(channel);
    }
}
