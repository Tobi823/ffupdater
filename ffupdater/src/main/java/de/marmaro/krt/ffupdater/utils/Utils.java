package de.marmaro.krt.ffupdater.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Tobiwan on 29.04.2020.
 */
public class Utils {

    @NonNull
    public static String convertNullToEmptyString(@Nullable String string) {
        if (string == null) {
            return "";
        }
        return string;
    }
}
