package de.marmaro.krt.ffupdater.background;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import de.marmaro.krt.ffupdater.FirefoxDetector;
import de.marmaro.krt.ffupdater.LocalInstalledVersions;
import de.marmaro.krt.ffupdater.MainActivity;
import de.marmaro.krt.ffupdater.MobileVersions;
import de.marmaro.krt.ffupdater.MozillaApiConsumer;
import de.marmaro.krt.ffupdater.R;
import de.marmaro.krt.ffupdater.VersionCompare;

/**
 * This class checks if a new firefox release is available.
 * If a new version is available, an update notification will be created.
 */
public class UpdateNotifierService extends IntentService {
    public static final String SERVICE_NAME = "UpdateNotifierService";
    public static final int UPDATE_NOTIFICATION_ID = 1;
    public static final int REQUEST_CODE_START_MAIN_ACTIVITY = 2;

    private static final String TAG = SERVICE_NAME;

    public UpdateNotifierService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "UpdateNotifierService was started.");
        if (isUpdateAvailable()) {
            showNotification();
        }
    }

    protected boolean isUpdateAvailable() {
        FirefoxDetector finder = FirefoxDetector.create(getPackageManager());

        LocalInstalledVersions current = finder.getLocalVersions();
        MobileVersions latest = getLatestMobileVersions();

        return !VersionCompare.isUpdateAvailable(latest, current).isEmpty();
    }

    protected MobileVersions getLatestMobileVersions() {
        return MozillaApiConsumer.findCurrentMobileVersions();
    }

    protected void showNotification() {
        // access the big app icon as bitmap
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        // open main view when notification was pressed
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.OPENED_BY_NOTIFICATION, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE_START_MAIN_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.transparent, 0)
                .setLargeIcon(largeIcon)
                .setContentTitle(getString(R.string.update_notification_title))
                .setContentText(getString(R.string.update_notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        getNotificationManager().notify(UPDATE_NOTIFICATION_ID, notification);
    }

    protected NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
