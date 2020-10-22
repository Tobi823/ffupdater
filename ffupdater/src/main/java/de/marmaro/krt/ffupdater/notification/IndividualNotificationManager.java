package de.marmaro.krt.ffupdater.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.InstallActivity;
import de.marmaro.krt.ffupdater.R;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

class IndividualNotificationManager {
    public static final String CHANNEL_ID_BASE = "update_notification_channel__";
    public static final int NOTIFICATION_ID_BASE = 200;

    private final Map<App, String> channelIds = new HashMap<>();
    private final Map<App, Integer> notificationIds = new HashMap<>();

    private final Context context;
    private final NotificationManager notificationManager;

    IndividualNotificationManager(Context context, NotificationManager notificationManager) {
        this.context = Objects.requireNonNull(context);
        this.notificationManager = Objects.requireNonNull(notificationManager);
        for (App app : App.values()) {
            channelIds.put(app, CHANNEL_ID_BASE + app.name().toLowerCase());
            notificationIds.put(app, NOTIFICATION_ID_BASE + app.ordinal());
        }
    }

    void hideNotifications() {
        for (App app : App.values()) {
            hideNotification(app);
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
        final String appTitle = app.getTitle(context);

        Intent intent = new Intent(context, InstallActivity.class);
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name());
        final PendingIntent updateAppIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT);

        new NotificationBuilder()
                .setContext(context)
                .setNotificationManager(notificationManager)
                .setChannelId(channelIds.get(app))
                .setChannelName(context.getString(R.string.update_notification_channel_individual_name, appTitle))
                .setChannelDescription(context.getString(R.string.update_notification_channel_individual_description, appTitle))
                .setNotificationId(Objects.requireNonNull(notificationIds.get(app)))
                .setNotificationTitle(context.getString(R.string.notification_individual_title, appTitle))
                .setNotificationText(context.getString(R.string.notification_individual_text))
                .setPendingIntent(updateAppIntent)
                .showNotification();
    }
}
