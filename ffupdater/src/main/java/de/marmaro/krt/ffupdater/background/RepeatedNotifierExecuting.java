package de.marmaro.krt.ffupdater.background;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is responsible for executing {@link UpdateNotifierService} every 3 hours.
 */
public class RepeatedNotifierExecuting {
    public static final long INTERVAL_DURATION = AlarmManager.INTERVAL_HOUR * 3;
    private static final String TAG = "RepeatedN_Executing";
    private static final int REQUEST_CODE_CHECK_FOR_UPDATE = 3;

    public static void register(Context context) {
        PendingIntent alarmIntent = createIntent(context);
        createRepeatingAlarm(context, alarmIntent, INTERVAL_DURATION);
    }

    private static PendingIntent createIntent(Context context) {
        Intent intent = new Intent(context, UpdateNotifierService.class);
        return PendingIntent.getService(context, REQUEST_CODE_CHECK_FOR_UPDATE, intent, 0);
    }

    private static void createRepeatingAlarm(Context context, PendingIntent pendingIntent, long timePeriod) {
        AlarmManager am = getAlarmManager(context);
        long nextExecution = SystemClock.elapsedRealtime() + timePeriod;

        // register alarm
        Log.i(TAG, "register inexact alarm.");
        //  If there is already an alarm scheduled for the same IntentSender, it will first be canceled.
        // (only for the setRepeating method but i think it also applies to setInexactRepeating
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, nextExecution, timePeriod, pendingIntent);
        // AlarmManager.ELAPSED_REALTIME => when the device is wake (screen on),
        // then every x seconds the pendingIntent will be executed. This method is not very precise
        // but energy saving (see https://developer.android.com/training/scheduling/alarms.html#type)
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}
