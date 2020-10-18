package de.marmaro.krt.ffupdater.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class with useful helper methods.
 */
public class Utils {
    private static final String LOG_TAG = "Utils";

    /**
     * @param string   string
     * @param fallback fallback
     * @return convert the string to an int or return fallback
     */
    public static int stringToInt(@Nullable String string, int fallback) {
        if (string == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(string);
        } catch (final NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * @param string string
     * @return convert the string to an int or throw exception
     * @throws RuntimeException exception
     */
    public static int stringToInt(@Nullable String string) {
        if (string == null) {
            throw new RuntimeException("could not convert string to int");
        }
        try {
            return Integer.parseInt(string);
        } catch (final NumberFormatException nfe) {
            throw new RuntimeException("could not convert string to int");
        }
    }

    /**
     * @param string string
     * @return if string is null, then return empty string.
     * if string is not null, return string.
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

    private static final String[] versionAndCodenames = new String[]{
            "1.0",
            "1.1",
            "1.5 Cupcake",
            "1.6 (Donut)",
            "2.0 (Eclair)",
            "2.0.1 (Eclair)",
            "2.1 (Eclair)",
            "2.2 (Froyo)",
            "2.3 (Gingerbread)",
            "2.3.3 (Gingerbread)",
            "3.0 (Honeycomb)",
            "3.1 (Honeycomb)",
            "3.2 (Honeycomb)",
            "4.0.1 (Ice Cream Sandwich)",
            "4.0.3 (Ice Cream Sandwich)",
            "4.1 (Jelly Bean)",
            "4.2 (Jelly Bean)",
            "4.3 (Jelly Bean)",
            "4.4 (KitKat)",
            "4.4W (KitKat)",
            "5.0 (Lollipop)",
            "5.1 (Lollipop)",
            "6.0 (Marshmallow)",
            "7.0 (Nougat)",
            "7.1 (Nougat)",
            "8.0.0 (Oreo)",
            "8.1.0 (Oreo)",
            "9 (Pie)",
            "10",
            "11"
    };

    /**
     * @param apiLevel API Level
     * @return the Android version an its codename for the associated API Level
     */
    public static String getVersionAndCodenameFromApiLevel(int apiLevel) {
        if (apiLevel <= 0 || apiLevel > versionAndCodenames.length) {
            return "API Level " + apiLevel;
        }
        return versionAndCodenames[apiLevel - 1];
    }

    /**
     * @param collection collection
     * @return the collection as CharSequence array
     */
    @NonNull
    public static CharSequence[] stringsToCharSequenceArray(@NonNull Collection<String> collection) {
        CharSequence[] result = new CharSequence[collection.size()];
        int i = 0;
        for (String element : collection) {
            result[i++] = element;
        }
        return result;
    }

    /**
     * @param collection collection
     * @return the collection as CharSequence array
     */
    @NonNull
    public static CharSequence[] integersToCharSequenceArray(@NonNull Collection<Integer> collection) {
        CharSequence[] result = new CharSequence[collection.size()];
        int i = 0;
        for (Integer element : collection) {
            result[i++] = String.valueOf(element);
        }
        return result;
    }

    public static <T> Set<T> createSet(T element) {
        Set<T> set = new HashSet<>();
        set.add(element);
        return set;
    }

    public static <K, V> Map<K, V> createMap(K key, V value) {
        final Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new ParamRuntimeException("invalid url");
        }
    }
}
