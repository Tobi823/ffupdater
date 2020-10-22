package de.marmaro.krt.ffupdater.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.MainActivity;
import de.marmaro.krt.ffupdater.R;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

class SummarizedNotificationManager {
    public static final String CHANNEL_ID = "update_notification_channel__all";
    public static final int NOTIFICATION_ID = 100;

    private final Context context;
    private final NotificationManager notificationManager;

    SummarizedNotificationManager(Context context, NotificationManager notificationManager) {
        this.context = Objects.requireNonNull(context);
        this.notificationManager = Objects.requireNonNull(notificationManager);
    }

    void hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    void showNotification(List<App> apps) {
        if (apps.isEmpty()) {
            hideNotification();
            return;
        }

        String appsAsString = apps.stream().map(app -> app.getTitle(context)).collect(Collectors.joining(", "));
        Intent intent = new Intent(context, MainActivity.class);
        final PendingIntent openMainMenuIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT);

        new NotificationBuilder()
                .setContext(context)
                .setNotificationManager(notificationManager)
                .setChannelId(CHANNEL_ID)
                .setChannelName(context.getString(R.string.update_notification_channel_summarized_name))
                .setChannelDescription(context.getString(R.string.update_notification_channel_summarized_description))
                .setNotificationId(NOTIFICATION_ID)
                .setNotificationTitle(context.getString(R.string.notification_summarized_title, appsAsString))
                .setNotificationText(context.getString(R.string.notification_summarized_text))
                .setPendingIntent(openMainMenuIntent)
                .showNotification();


    }
}
