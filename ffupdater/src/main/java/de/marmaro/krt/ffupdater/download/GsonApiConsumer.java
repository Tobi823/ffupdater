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
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Tobiwan on 05.04.2020.
 */
class GsonApiConsumer {
    private static final String TAG = "ffupdater";
    public static final int TIMEOUT = 5000;

    @Nullable
    static <T> T consume(String urlString, Class<T> clazz) {
        HttpsURLConnection urlConnection;
        try {
            urlConnection = (HttpsURLConnection) new URL(urlString).openConnection();
        } catch (IOException e) {
            Log.e(TAG, "Can't connect to API interface", e);
            return null;
        }
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setReadTimeout(TIMEOUT);
        try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream()))) {
            return new Gson().fromJson(reader, clazz);
        } catch (IOException e) {
            Log.e(TAG, "Can't consume API interface", e);
            return null;
        } finally {
            urlConnection.disconnect();
        }
    }
}
