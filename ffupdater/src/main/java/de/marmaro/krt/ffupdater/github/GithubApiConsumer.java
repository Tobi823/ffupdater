package de.marmaro.krt.ffupdater.github;

import android.util.Log;

import com.github.dmstocking.optional.java.util.Optional;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by Tobiwan on 20.07.2018.
 * This class can consume the API from Github containing the information about the latest
 * (real, not pre-release) release.
 */
public class GithubApiConsumer {
    private static final String TAG = "ffupdater";

    private static final String GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String FIREFOX_KLAR_OWNER = "mozilla-mobile";
    private static final String FIREFOX_KLAR_REPO = "focus-android";

    private static Optional<String> consumeApiLatestRelease() {
        try {
            String urlString = String.format(GITHUB_API_LATEST_RELEASE, FIREFOX_KLAR_OWNER, FIREFOX_KLAR_REPO);
            URL url = new URL(urlString);
            try (InputStream is = url.openConnection().getInputStream()) {
                String value = IOUtils.toString(is, StandardCharsets.UTF_8.name());
                return Optional.ofNullable(value);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e);
            return Optional.empty();
        }
    }

    public static Optional<Release> findLatestRelease() {
        Optional<String> result = consumeApiLatestRelease();
        if (result.isPresent()) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Release release = gsonBuilder.create().fromJson(result.get(), Release.class);
            return Optional.ofNullable(release);
        }
        return Optional.empty();
    }
}
