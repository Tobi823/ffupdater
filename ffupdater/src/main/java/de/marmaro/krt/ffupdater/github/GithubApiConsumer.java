package de.marmaro.krt.ffupdater.github;

import com.github.dmstocking.optional.java.util.Optional;

import de.marmaro.krt.ffupdater.ApiConsumer;

/**
 * Created by Tobiwan on 20.07.2018.
 * This class can consume the API from Github containing the information about the latest
 * (real, not pre-release) release.
 */
public class GithubApiConsumer {
    private static final String GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String FIREFOX_KLAR_OWNER = "mozilla-mobile";
    private static final String FIREFOX_KLAR_REPO = "focus-android";

    public static Optional<Release> findLatestRelease() {
        String url = String.format(GITHUB_API_LATEST_RELEASE, FIREFOX_KLAR_OWNER, FIREFOX_KLAR_REPO);
        return ApiConsumer.findApiResponse(url, Release.class);
    }
}
