package de.marmaro.krt.ffupdater.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import de.marmaro.krt.ffupdater.R;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static de.marmaro.krt.ffupdater.R.mipmap.transparent;

public class ErrorNotificationManager {
    public static final String CHANNEL_ID = "error_notification_channel";
    public static final int NOTIFICATION_ID = 300;
    public static final String COPY_TO_CLIPBOARD_INTENT = "de.marmaro.krt.ffupdater.notification.ErrorNotificationManager.ACTION_COPY";

    private final Context context;
    private final NotificationManager notificationManager;

    ErrorNotificationManager(Context context, NotificationManager notificationManager) {
        this.context = Objects.requireNonNull(context);
        this.notificationManager = Objects.requireNonNull(notificationManager);
    }

    public void showNotification(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        final BroadcastReceiver copyToClipboard = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ClipboardManager clipboard = (ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("FFUpdater error", sw.toString()));
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show();
            }
        };

        context.registerReceiver(copyToClipboard, new IntentFilter(COPY_TO_CLIPBOARD_INTENT));
        final Intent copyIntent = new Intent(COPY_TO_CLIPBOARD_INTENT);
        final PendingIntent updateAppIntent = PendingIntent.getBroadcast(context, 0, copyIntent, FLAG_UPDATE_CURRENT);

        final Notification notification = createNotificationBuilder()
                .setSmallIcon(transparent, 0)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("Background update check failed")
                .setContentText("Error: " + sw.toString())
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
                "Background errors",
                IMPORTANCE_DEFAULT);
        channel.setDescription("Show the error when the background update check fails");
        notificationManager.createNotificationChannel(channel);
    }
}
