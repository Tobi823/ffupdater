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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import de.marmaro.krt.ffupdater.R;
import james.crasher.activities.CrashActivity;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static de.marmaro.krt.ffupdater.R.mipmap.transparent;

public class ErrorNotificationManager {
    public static final String CHANNEL_ID = "error_notification_channel";
    public static final int NOTIFICATION_ID = 300;

    private final Context context;
    private final NotificationManager notificationManager;

    ErrorNotificationManager(Context context, NotificationManager notificationManager) {
        this.context = Objects.requireNonNull(context);
        this.notificationManager = Objects.requireNonNull(notificationManager);
    }

    public void showNotification(Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));

        final Intent intent = new Intent(context, CrashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CrashActivity.EXTRA_NAME, e.getClass().getName());
        intent.putExtra(CrashActivity.EXTRA_MESSAGE, e.getLocalizedMessage());
        intent.putExtra(CrashActivity.EXTRA_STACK_TRACE, stackTrace.toString());

        final PendingIntent updateAppIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT);
        final Notification notification = createNotificationBuilder()
                .setSmallIcon(transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.background_error_notification_title))
                .setContentText(context.getString(R.string.background_error_notification_text))
                .setContentIntent(updateAppIntent)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            return new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            //noinspection deprecation
            return new NotificationCompat.Builder(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        final NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.error_notification_channel_name),
                IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.error_notification_channel_description));
        notificationManager.createNotificationChannel(channel);
    }
}
