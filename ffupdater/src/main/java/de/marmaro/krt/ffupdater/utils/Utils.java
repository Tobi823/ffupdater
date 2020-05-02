package de.marmaro.krt.ffupdater.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class with useful helper methods.
 */
public class Utils {
    private static final String LOG_TAG = "Utils";

    /**
     * @param string string
     * @return if string is null, then return empty string.
     *         if string is not null, return string.
     */
    @NonNull
    public static String convertNullToEmptyString(@Nullable String string) {
        if (string == null) {
            return "";
        }
        return string;
    }

    /**
     * @param millis wait x milliseconds and ignore InterruptedException
     */
    public static void sleepAndIgnoreInterruptedException(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "failed sleep", e);
        }
    }
}
