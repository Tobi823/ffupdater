package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;


/**
 * Consume a REST-API from the internet.
 */
public class ApiConsumer {
    private static final String GZIP = "gzip";

    <T> T consume(URL url, Class<T> clazz) {
        try {
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Accept-Encoding", GZIP);
            urlConnection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            Preconditions.checkArgument(urlConnection instanceof HttpsURLConnection);

            if (GZIP.equals(urlConnection.getContentEncoding())) {
                try (InputStream inputStream = new GZIPInputStream(urlConnection.getInputStream())) {
                    return consume(inputStream, clazz);
                }
            } else {
                try (InputStream inputStream = urlConnection.getInputStream()) {
                    return consume(inputStream, clazz);
                }
            }
        } catch (IOException e) {
            throw new ParamRuntimeException(e, "can't consume API interface %s", url);
        }
    }

    private <T> T consume(InputStream inputStream, Class<T> clazz) throws IOException {
        try (BufferedReader buffered = new BufferedReader(new InputStreamReader(inputStream))) {
            return new Gson().fromJson(buffered, clazz);
        }
    }
}
