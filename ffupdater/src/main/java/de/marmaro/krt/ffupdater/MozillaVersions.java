package de.marmaro.krt.ffupdater;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MozillaVersions {
    private static final String TAG = "ffupdater";
    private static final String checkUri = "https://product-details.mozilla.org/1.0/mobile_versions.json";

    private static String downloadVersion() {
        HttpsURLConnection urlConnection = null;
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(checkUri);
            urlConnection = (HttpsURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e);
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
            String versionString = jObject.getString("version");
            version = new Version(versionString);
        } catch (JSONException e) {
            Log.e(TAG, "Error: " + e);
        }
        return version;
    }
}
