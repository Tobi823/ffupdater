package de.marmaro.krt.ffupdater;

import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

/**
 * Created by Tobiwan on 21.04.2020.
 */
public class StrictModeSetup {
    public static void enable() {
        if (BuildConfig.DEBUG) {
            enableDebugStrictMode();
        } else {
            enableReleaseStrictMode();
        }
    }

    private static void enableDebugStrictMode() {
        Log.i("MainActivity", "enable StrictMode for local development");
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
