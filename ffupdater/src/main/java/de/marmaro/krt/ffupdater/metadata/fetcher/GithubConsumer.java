package de.marmaro.krt.ffupdater.metadata.fetcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;
import de.marmaro.krt.ffupdater.utils.Utils;

class GithubConsumer {
    public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    public static final String ALL_RELEASES_URL = "https://api.github.com/repos/%s/%s/releases";

    private final ApiConsumer apiConsumer;

    GithubConsumer(ApiConsumer apiConsumer) {
        this.apiConsumer = apiConsumer;
    }

    @Nullable
    GithubResult consume(String owner, String repository) {
        Release release = findLatestRelease(owner, repository);
        if (release.getAssets() != null && !release.getAssets().isEmpty()) {
            return convert(release);
        }
        return convert(searchForLatestRelease(owner, repository));
    }

    private Release findLatestRelease(String owner, String repo) {
        final URL url = Utils.createURL(String.format(LATEST_RELEASE_URL, owner, repo));
        return apiConsumer.consume(url, Release.class);
    }

    private Release searchForLatestRelease(String owner, String repo) {
        final URL url = Utils.createURL(String.format(ALL_RELEASES_URL, owner, repo));
        for (Release release : apiConsumer.consume(url, Release[].class)) {
            if (release.getAssets() != null && !release.getAssets().isEmpty()) {
                return release;
            }
        }
        throw new ParamRuntimeException("missing real release in GitHub response");
    }

    private GithubResult convert(Release release) {
        final Map<String, URL> urls = new HashMap<>();
        try {
            for (Asset asset : release.getAssets()) {
                urls.put(asset.getName(), new URL(asset.getDownloadUrl()));
            }
        } catch (MalformedURLException e) {
            throw new ParamRuntimeException("invalid urls");
        }
        return new GithubResult(release.getTagName(), urls);
    }

    public static class GithubResult {
        private final String tagName;
        private final Map<String, URL> urls;

        public GithubResult(String tagName, Map<String, URL> urls) {
            this.tagName = tagName;
            this.urls = urls;
        }

        public String getTagName() {
            return tagName;
        }

        public Map<String, URL> getUrls() {
            return urls;
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

        @NonNull
        @Override
        public String toString() {
            return "Release{" +
                    "tagName='" + tagName + '\'' +
                    ", assets=" + assets +
                    '}';
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

        @NonNull
        @Override
        public String toString() {
            return "Asset{" +
                    "name='" + name + '\'' +
                    ", downloadUrl='" + downloadUrl + '\'' +
                    '}';
        }
    }
}
