package de.marmaro.krt.ffupdater.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class with useful helper methods.
 */
public class Utils {

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
}
