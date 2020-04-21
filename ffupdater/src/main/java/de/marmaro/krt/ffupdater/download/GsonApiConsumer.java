package de.marmaro.krt.ffupdater.download;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Tobiwan on 05.04.2020.
 */
class GsonApiConsumer {
    private static final String TAG = "ffupdater";

    @Nullable
    static <T> T consume(String urlString, Class<T> clazz) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL for API request", e);
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(url.openStream()))) {
            return new Gson().fromJson(reader, clazz);
        } catch (IOException e) {
            Log.e(TAG, "Can't consume API", e);
            return null;
        }
    }
}
