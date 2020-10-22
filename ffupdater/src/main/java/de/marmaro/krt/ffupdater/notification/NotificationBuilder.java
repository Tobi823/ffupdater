package de.marmaro.krt.ffupdater.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Objects;

import de.marmaro.krt.ffupdater.R;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static de.marmaro.krt.ffupdater.R.mipmap.transparent;

class NotificationBuilder {
    private Context context;
    private NotificationManager notificationManager;
    private String channelId;
    private String channelName;
    private String channelDescription;
    private int notificationId;
    private String notificationTitle;
    private String notificationText;
    private PendingIntent pendingIntent;

    public NotificationBuilder setContext(Context context) {
        this.context = context;
        return this;
    }

    public NotificationBuilder setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        return this;
    }

    public NotificationBuilder setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public NotificationBuilder setChannelName(String channelName) {
        this.channelName = channelName;
        return this;
    }

    public NotificationBuilder setChannelDescription(String channelDescription) {
        this.channelDescription = channelDescription;
        return this;
    }

    public NotificationBuilder setNotificationId(int notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public NotificationBuilder setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
        return this;
    }

    public NotificationBuilder setNotificationText(String notificationText) {
        this.notificationText = notificationText;
        return this;
    }

    public NotificationBuilder setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
        return this;
    }

    void showNotification() {
        Objects.requireNonNull(context);
        Objects.requireNonNull(notificationManager);
        Objects.requireNonNull(channelId);
        Objects.requireNonNull(channelName);
        Objects.requireNonNull(channelDescription);
        Objects.requireNonNull(notificationTitle);
        Objects.requireNonNull(notificationText);
        Objects.requireNonNull(pendingIntent);

        final NotificationCompat.Builder builder = createNotificationBuilder();
        final Notification notification = createNotification(builder);
        notificationManager.notify(notificationId, notification);
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(channelId, channelName, IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(channel);
            return new NotificationCompat.Builder(context, channelId);
        } else {
            //noinspection deprecation
            return new NotificationCompat.Builder(context);
        }
    }

    private Notification createNotification(NotificationCompat.Builder builder) {
        return builder
                .setSmallIcon(transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }
}
