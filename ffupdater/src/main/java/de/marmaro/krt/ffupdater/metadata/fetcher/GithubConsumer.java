package de.marmaro.krt.ffupdater.metadata.fetcher;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;
import de.marmaro.krt.ffupdater.utils.Utils;

public class GithubConsumer {
    public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    public static final String ALL_RELEASES_URL = "https://api.github.com/repos/%s/%s/releases?per_page=%d&page=%d";

    private final ApiConsumer apiConsumer;

    public GithubConsumer(ApiConsumer apiConsumer) {
        Objects.requireNonNull(apiConsumer);
        this.apiConsumer = apiConsumer;
    }

    GithubResult consumeLatestReleaseFirst(Request request) {
        Preconditions.checkArgument(isRequestValid(request));
        final URL url = Utils.createURL(String.format(
                LATEST_RELEASE_URL,
                request.ownerOfRepository,
                request.repositoryName));
        Release release = apiConsumer.consume(url, Release.class);
        if (request.releaseValidator.test(release)) {
            return convert(release);
        }
        return consumeManyReleases(request);
    }

    GithubResult consumeManyReleases(Request request) {
        Preconditions.checkArgument(isRequestValid(request));
        for (int page = 1; page < 5; page++) {
            final URL url = Utils.createURL(String.format(
                    Locale.getDefault(),
                    ALL_RELEASES_URL,
                    request.ownerOfRepository,
                    request.repositoryName,
                    request.resultsPerPage,
                    page));
            final Release[] test = apiConsumer.consume(url, Release[].class);
            for (Release release : test) {
                if (request.releaseValidator.test(release)) {
                    return convert(release);
                }
            }
        }
        throw new ParamRuntimeException("can't find release after nine network requests - abort");
    }

    private boolean isRequestValid(Request request) {
        return request != null
                && request.ownerOfRepository != null
                && request.repositoryName != null
                && request.resultsPerPage != -1
                && request.releaseValidator != null;
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

    public static class Request {
        private String ownerOfRepository;
        private String repositoryName;
        private int resultsPerPage = -1;
        private Predicate<Release> releaseValidator;

        public Request setOwnerOfRepository(String ownerOfRepository) {
            this.ownerOfRepository = ownerOfRepository;
            return this;
        }

        public Request setRepositoryName(String repositoryName) {
            this.repositoryName = repositoryName;
            return this;
        }

        public Request setResultsPerPage(int resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
            return this;
        }

        public Request setReleaseValidator(Predicate<Release> releaseValidator) {
            this.releaseValidator = releaseValidator;
            return this;
        }
    }

    static class GithubResult {
        private final String tagName;
        private final Map<String, URL> urls;

        GithubResult(String tagName, Map<String, URL> urls) {
            this.tagName = tagName;
            this.urls = urls;
        }

        String getTagName() {
            return tagName;
        }

        Map<String, URL> getUrls() {
            return urls;
        }
    }

    static class Release {
        @SerializedName("tag_name")
        private String tagName;

        @SerializedName("name")
        private String name;

        @SerializedName("prerelease")
        private boolean prerelease;

        @SerializedName("assets")
        private List<Asset> assets;

        public String getTagName() {
            return tagName;
        }

        public String getName() {
            return name;
        }

        public boolean isPrerelease() {
            return prerelease;
        }

        public List<Asset> getAssets() {
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

    static class Asset {
        @SerializedName("name")
        private String name;

        @SerializedName("browser_download_url")
        private String downloadUrl;

        String getName() {
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
