package de.marmaro.krt.ffupdater;

import android.util.Log;

import com.github.dmstocking.optional.java.util.Optional;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by Tobiwan on 26.07.2018.
 */
public class ApiConsumer {
    private static final String TAG = "ffupdater";

    public static Optional<String> findRawApiResponse(String url) {
        try {
            URL urlObject = new URL(url);
            try (InputStream is = urlObject.openConnection().getInputStream()) {
                String value = IOUtils.toString(is, StandardCharsets.UTF_8.name());
                return Optional.ofNullable(value);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static <T> Optional<T> findApiResponse(String url, Class<T> clazz) {
        Optional<String> result = findRawApiResponse(url);
        if (result.isPresent()) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            T release = gsonBuilder.create().fromJson(result.get(), clazz);
            return Optional.ofNullable(release);
        }
        return Optional.empty();
    }
}
