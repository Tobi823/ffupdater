package de.marmaro.krt.ffupdater.version;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;


/**
 * Consume a REST-API from the internet.
 */
class ApiConsumer {
    private static final String LOG_TAG = "ffupdater";
    private static final int TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    public static final String GZIP = "gzip";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * Download the JSON respond from url and convert the JSON result to a Java object.
     * <p>
     * Request the server for gzip-compression. <a href="https://stackoverflow.com/a/27720902">Link</a>
     * Cons: This may help an attacker to guess the plaintext of the encrypted HTTPS connection due
     * to gzip. But the plaintext is not secret because everyone can access it by calling the URLs.
     * Pros: Saves network traffic.
     *
     * @param url   url
     * @param clazz JSON result will be parsed to an instance of this clazz
     * @param <T>   clazz
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
        urlConnection.setRequestProperty(ACCEPT_ENCODING, GZIP);
        urlConnection.setConnectTimeout(TIMEOUT);

        String contentEncoding = urlConnection.getContentEncoding();
        boolean gzipped = GZIP.equals(contentEncoding);
        Preconditions.checkArgument(gzipped || contentEncoding == null);

        try (InputStream original = urlConnection.getInputStream();
             InputStream decompressed = gzipped ? new GZIPInputStream(original) : original;
             InputStreamReader reader = new InputStreamReader(decompressed);
             BufferedReader buffered = new BufferedReader(reader)) {
            return new Gson().fromJson(buffered, clazz);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't consume API interface", e);
            return null;
        } finally {
            urlConnection.disconnect();
        }
    }
}
