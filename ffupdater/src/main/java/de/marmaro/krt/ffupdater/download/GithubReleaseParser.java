package de.marmaro.krt.ffupdater.download;

import android.util.Log;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Access the version name and the download url from Github.
 */
class GithubReleaseParser {
    private static final String TAG = "ffupdater";
    private static final String UTF_8 = "UTF-8";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String ALL_RELEASES_URL = "https://api.github.com/repos/%s/%s/releases";

    static Optional<Release> findLatestRelease(String owner, String repo) {
        Optional<Release> release = findLatestReleaseByApi(owner, repo);
        if (release.isPresent() &&
                release.get().getAssets() != null &&
                !release.get().getAssets().isEmpty()) {
            return Optional.of(release.get());
        }
        return findLatestReleaseBySearchingAllReleases(owner, repo);
    }

    private static Optional<Release> findLatestReleaseByApi(String owner, String repo) {
        String downloadUrl = String.format(LATEST_RELEASE_URL, owner, repo);
        Optional<String> json = fetchData(downloadUrl);
        if (!json.isPresent()) {
            return Optional.absent();
        }

        Gson gson = new Gson();
        return Optional.of(gson.fromJson(json.get(), Release.class));
    }

    private static Optional<Release> findLatestReleaseBySearchingAllReleases(String owner, String repo) {
        String downloadUrl = String.format(ALL_RELEASES_URL, owner, repo);
        Optional<String> json = fetchData(downloadUrl);
        if (!json.isPresent()) {
            return Optional.absent();
        }

        Gson gson = new Gson();
        Release[] releases = gson.fromJson(json.get(), Release[].class);
        for (Release release : releases) {
            if (release.getAssets() != null && !release.getAssets().isEmpty()) {
                return Optional.of(release);
            }
        }
        return Optional.absent();
    }

    private static Optional<String> fetchData(String url) {
        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) new URL(url).openConnection();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return Optional.of(IOUtils.toString(inputStream, UTF_8));
            }
        } catch (IOException e) {
            Log.e(TAG, "cant get latest release from Github", e);
            return Optional.absent();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static class Asset {
        @SerializedName("name")
        private String name;

        @SerializedName("browser_download_url")
        private String downloadUrl;

        public String getName() {
            return name;
        }

        String getDownloadUrl() {
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

    public static class Release {
        @SerializedName("tag_name")
        private String tagName;

        @SerializedName("assets")
        private List<Asset> assets;

        String getTagName() {
            return tagName;
        }

        List<Asset> getAssets() {
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
