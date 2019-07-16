package de.marmaro.krt.ffupdater;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class MozillaVersions {
    private static final String TAG = "ffupdater";
    private static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";
    private static final String UTF_8 = "UTF-8";

    private static String downloadVersion() {
        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) new URL(CHECK_URL).openConnection();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return IOUtils.toString(inputStream, UTF_8);
            }
        } catch (IOException e) {
            Log.e(TAG, "cant get latest firefox versions", e);
            return "";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static Version getVersion() {
        String result = downloadVersion();
        Version version = null;
        JSONObject jObject;
        try {
            jObject = new JSONObject(result);
            String versionString = jObject.getString(UpdateChannel.channel);
            version = new Version(versionString);
        } catch (JSONException e) {
            Log.e(TAG, "Error: " + e);
        }
        return version;
    }

    public static Response getResponse() {
        String json = downloadVersion();
        Gson gson = new Gson();
        return gson.fromJson(json, Response.class);
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
