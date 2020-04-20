package de.marmaro.krt.ffupdater.download;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Tobiwan on 05.04.2020.
 */
class GsonApiConsumer {
    private static final String TAG = "ffupdater";

    @Nullable
    static <T> T consume(String url, Class<T> clazz) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                Log.e(TAG, "fail to enable TSLv1.2 necessary for Github", e);
                return null;
            }
        }

        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) new URL(url).openConnection();
            try (InputStream is = urlConnection.getInputStream();
                 BufferedInputStream buffered = new BufferedInputStream(is);
                 InputStreamReader reader = new InputStreamReader(buffered)) {
                Gson gson = new Gson();
                return gson.fromJson(reader, clazz);
            }
        } catch (IOException e) {
            Log.e(TAG, "can't consume api", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
