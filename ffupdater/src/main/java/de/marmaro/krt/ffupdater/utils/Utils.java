package de.marmaro.krt.ffupdater.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class with useful helper methods.
 */
public class Utils {

    public static final String LOG_TAG = "Utils";

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

    public static void sleepAndIgnoreInterruptedException(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "failed sleep", e);
        }
    }
}
