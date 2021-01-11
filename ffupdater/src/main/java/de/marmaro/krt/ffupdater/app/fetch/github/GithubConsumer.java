package de.marmaro.krt.ffupdater.app.fetch.github;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import de.marmaro.krt.ffupdater.app.fetch.ApiConsumer;
import de.marmaro.krt.ffupdater.app.fetch.github.dao.Asset;
import de.marmaro.krt.ffupdater.app.fetch.github.dao.Result;
import de.marmaro.krt.ffupdater.app.fetch.github.dao.Release;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;
import de.marmaro.krt.ffupdater.utils.Utils;

public class GithubConsumer {
    private final ApiConsumer apiConsumer;
    private final URL latestRelease;
    private final String allReleasesTemplate;
    private final Predicate<Release> validReleaseTester;
    private final Predicate<Asset> correctDownloadUrlTester;

    private GithubConsumer(ApiConsumer apiConsumer,
                           URL latestRelease,
                           String allReleasesTemplate,
                           Predicate<Release> validReleaseTester, Predicate<Asset> correctDownloadUrlTester) {
        this.latestRelease = Objects.requireNonNull(latestRelease);
        this.allReleasesTemplate = Objects.requireNonNull(allReleasesTemplate);
        this.apiConsumer = Objects.requireNonNull(apiConsumer);
        this.validReleaseTester = Objects.requireNonNull(validReleaseTester);
        this.correctDownloadUrlTester = Objects.requireNonNull(correctDownloadUrlTester);
    }

    public Result updateCheck() {
        return updateCheckLatestRelease().orElseGet(this::updateCheckAllReleases);
    }

    /**
     * Optional -> if result is not valid (no check for errors)
     * @return
     */
    Optional<Result> updateCheckLatestRelease() {
        final Release release = apiConsumer.consume(latestRelease, Release.class);
        return Optional.of(release)
                .filter(validReleaseTester)
                .map(this::convert);
    }

    Result updateCheckAllReleases() {
        final int tries = 4;
        for (int page = 1; page < (tries + 1); page++) {
            final URL url = Utils.createURL(String.format(allReleasesTemplate, page));
            final Release[] releases = apiConsumer.consume(url, Release[].class);
            final Optional<Result> result = Arrays.stream(releases)
                    .filter(validReleaseTester)
                    .findFirst()
                    .map(this::convert);
            if (result.isPresent()) {
                return result.get();
            }
        }
        throw new ParamRuntimeException("can't find release after " + tries + " tries - abort");
    }

    private Result convert(Release release) {
        for (Asset asset : release.getAssets()) {
            if (correctDownloadUrlTester.test(asset)) {
                URL url = Utils.createURL(asset.getDownloadUrl());
                return new Result(release.getTagName(), url);
            }
        }
        throw new IllegalStateException("The valid release doesn't have an asset with the correct download url");
    }

    public static class Builder {
        public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/%s/%s/releases/latest";
        // the value page will be replaced first with "%d" and then with a number
        public static final String ALL_RELEASES_URL = "https://api.github.com/repos/%s/%s/releases?per_page=%d&page=%s";

        private ApiConsumer apiConsumer;
        private String repoOwner;
        private String repoName;
        private int resultsPerPage = -1;
        private Predicate<Release> validReleaseTester;
        private Predicate<Asset> correctDownloadUrlTester;

        public Builder setApiConsumer(ApiConsumer apiConsumer) {
            this.apiConsumer = apiConsumer;
            return this;
        }

        public Builder setRepoOwner(String repoOwner) {
            this.repoOwner = repoOwner;
            return this;
        }

        public Builder setRepoName(String repoName) {
            this.repoName = repoName;
            return this;
        }

        public Builder setResultsPerPage(int resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
            return this;
        }

        public Builder setValidReleaseTester(Predicate<Release> releaseValidator) {
            this.validReleaseTester = releaseValidator;
            return this;
        }

        public Builder setCorrectDownloadUrlTester(Predicate<Asset> correctDownloadUrlTester) {
            this.correctDownloadUrlTester = correctDownloadUrlTester;
            return this;
        }

        public GithubConsumer build() {
            Objects.requireNonNull(apiConsumer, "call setApiConsumer() first");
            Objects.requireNonNull(repoOwner, "call setRepoOwner() first");
            Objects.requireNonNull(repoName, "call setRepoName() first");
            Preconditions.checkArgument(resultsPerPage != -1);
            Objects.requireNonNull(validReleaseTester, "call setReleaseValidator() first");
            Objects.requireNonNull(correctDownloadUrlTester, "call setCorrectDownloadUrlTester() first");

            final URL latestRelease = Utils.createURL(String.format(LATEST_RELEASE_URL, repoOwner, repoName));
            final String allReleasesTemplate = String.format(Locale.getDefault(), ALL_RELEASES_URL,
                    repoOwner, repoName, resultsPerPage, "%d");

            return new GithubConsumer(apiConsumer, latestRelease, allReleasesTemplate, validReleaseTester, correctDownloadUrlTester);
        }
    }
}
