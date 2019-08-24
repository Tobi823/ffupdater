package de.marmaro.krt.ffupdater.download.fennec;

import android.util.Log;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class FennecVersionFinder {
    private static final String TAG = "ffupdater";
    private static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";
    private static final String UTF_8 = "UTF-8";

    private static Optional<String> downloadVersion() {
        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) new URL(CHECK_URL).openConnection();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return Optional.of(IOUtils.toString(inputStream, UTF_8));
            }
        } catch (IOException e) {
            Log.e(TAG, "cant getVersion latest firefox versions", e);
            return Optional.absent();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static Optional<Response> getResponse() {
        Optional<String> json = downloadVersion();
        if (!json.isPresent()) {
            return Optional.absent();
        }
        Gson gson = new Gson();
        return Optional.of(gson.fromJson(json.get(), Response.class));
    }

    public static class Response {
        @SerializedName("version")
        private String releaseVersion;

        @SerializedName("beta_version")
        private String betaVersion;

        @SerializedName("nightly_version")
        private String nightlyVersion;

        public String getReleaseVersion() {
            return releaseVersion;
        }

        public String getBetaVersion() {
            return betaVersion;
        }

        public String getNightlyVersion() {
            return nightlyVersion;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "releaseVersion='" + releaseVersion + '\'' +
                    ", betaVersion='" + betaVersion + '\'' +
                    ", nightlyVersion='" + nightlyVersion + '\'' +
                    '}';
        }
    }
}
