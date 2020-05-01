package de.marmaro.krt.ffupdater.version;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Consume a REST-API from the internet.
 */
class GsonApiConsumer {
    private static final String LOG_TAG = "ffupdater";
    private static final int TIMEOUT = 5000;

    /**
     * Download the JSON respond from url and convert the JSON result to a Java object.
     * @param url url
     * @param clazz JSON result will be parsed to an instance of this clazz
     * @param <T> clazz
     * @return result or null (if an error occurs)
     */
    @Nullable
    static <T> T consume(String url, Class<T> clazz) {
        HttpsURLConnection urlConnection;
        try {
            urlConnection = (HttpsURLConnection) new URL(url).openConnection();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't connect to API interface", e);
            return null;
        }
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setReadTimeout(TIMEOUT);
        try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream()))) {
            return new Gson().fromJson(reader, clazz);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't consume API interface", e);
            return null;
        } finally {
            urlConnection.disconnect();
        }
    }
}
