package de.marmaro.krt.ffupdater.notification;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.marmaro.krt.ffupdater.settings.SettingsHelper;

import static androidx.work.ExistingPeriodicWorkPolicy.REPLACE;

public class BackgroundUpdateCheckerCreator {
    private static final String WORK_MANAGER_KEY = "update_checker";
    private final SettingsHelper settingsHelper;
    private final WorkManager workManager;

    public BackgroundUpdateCheckerCreator(Context context) {
        Objects.requireNonNull(context);
        settingsHelper = new SettingsHelper(context);
        workManager = Objects.requireNonNull(WorkManager.getInstance(context));
    }

    public void startOrStopBackgroundUpdateCheck() {
        if (settingsHelper.isAutomaticCheck()) {
            startBackgroundUpdateCheck();
        } else {
            stopBackgroundUpdateCheck();
        }
    }

    private void startBackgroundUpdateCheck() {
        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
        final PeriodicWorkRequest saveRequest = new PeriodicWorkRequest.Builder(
                BackgroundUpdateChecker.class, settingsHelper.getCheckInterval().toMinutes(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        workManager.enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest);
    }

    private void stopBackgroundUpdateCheck() {
        workManager.cancelUniqueWork(WORK_MANAGER_KEY);
    }
}
