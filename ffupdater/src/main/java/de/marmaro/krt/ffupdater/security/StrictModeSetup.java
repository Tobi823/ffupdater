package de.marmaro.krt.ffupdater.security;

import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import de.marmaro.krt.ffupdater.BuildConfig;

/**
 * Configure StrictMode to improve security (by prohibit unencrypted network traffic) and detect bugs during development.
 */
public class StrictModeSetup {
    private static final String LOG_TAG = "StrictModeSetup";

    /**
     * If the app has been built locally, then StrictMode will be configured very repressive. This is necessary to find bugs in development fast.
     * If the app has been built by F-Droid, then forbid only unencrypted network traffic.
     */
    public static void enable() {
        if (BuildConfig.DEBUG) {
            enableDebugStrictMode();
        } else {
            enableReleaseStrictMode();
        }
    }

    private static void enableDebugStrictMode() {
        Log.i(LOG_TAG, "enable StrictMode for local development");
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads() // for preferences
                .permitDiskWrites() // for update
                .permitNetwork() // for checking updates
                .penaltyLog()
                .penaltyDeath()
                .build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private static void enableReleaseStrictMode() {
        Log.i("MainActivity", "enable StrictMode for everyday usage to prevent unencrypted data connection");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .penaltyDeathOnCleartextNetwork()
                    .build());
        }
    }
}
