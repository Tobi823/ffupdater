package de.marmaro.krt.ffupdater.download.github;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Tobiwan on 22.08.2019.
 */
public class LatestReleaseSearcher {
    private static final String TAG = "ffupdater";
    private static final String UTF_8 = "UTF-8";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String ALL_RELEASES_URL = "https://api.github.com/repos/%s/%s/releases";

    public static Release findLatestRelease(String owner, String repo) {
        Release release = findLatestReleaseViaApi(owner, repo);
        if (release != null && release.getAssets() != null && !release.getAssets().isEmpty()) {
            return release;
        }
        return findLatestReleaseBySearchingAllReleases(owner, repo);
    }

    private static Release findLatestReleaseViaApi(String owner, String repo) {
        String downloadUrl = String.format(LATEST_RELEASE_URL, owner, repo);
        String json = fetchData(downloadUrl);
        Log.i("download", "heruntergeladen: " + json.length());
        Gson gson = new Gson();
        return gson.fromJson(json, Release.class);
    }

    private static Release findLatestReleaseBySearchingAllReleases(String owner, String repo) {
        String downloadUrl = String.format(ALL_RELEASES_URL, owner, repo);
        String json = fetchData(downloadUrl);
        Log.i("download", "heruntergeladen: " + json.length());
        Gson gson = new Gson();
        Release[] releases = gson.fromJson(json, Release[].class);
        for (Release release : releases) {
            if (release.getAssets() != null && !release.getAssets().isEmpty()) {
                return release;
            }
        }
        throw new IllegalArgumentException("cant find a release with assets");
    }

    private static String fetchData(String url) {
        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) new URL(url).openConnection();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return IOUtils.toString(inputStream, UTF_8);
            }
        } catch (IOException e) {
            Log.e(TAG, "cant get latest release from Github", e);
            return "";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Created by Tobiwan on 23.08.2019.
     */
    public static class Asset {
        @SerializedName("name")
        private String name;

        @SerializedName("browser_download_url")
        private String downloadUrl;

        public String getName() {
            return name;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        @Override
        public String toString() {
            return "Asset{" +
                    "name='" + name + '\'' +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }
    }

    /**
     * Created by Tobiwan on 23.08.2019.
     */
    public static class Release {
        @SerializedName("tag_name")
        private String tagName;

        @SerializedName("assets")
        private List<Asset> assets;

        public String getTagName() {
            return tagName;
        }

        public List<Asset> getAssets() {
            return assets;
        }

        @Override
        public String toString() {
            return "Release{" +
                    "tagName='" + tagName + '\'' +
                    ", assets=" + assets +
                    '}';
        }
    }
}
