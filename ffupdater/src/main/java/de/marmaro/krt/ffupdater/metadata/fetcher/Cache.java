package de.marmaro.krt.ffupdater.metadata.fetcher;

import android.content.SharedPreferences;

import com.google.common.base.Preconditions;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.utils.CompareHelper;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;
import de.marmaro.krt.ffupdater.metadata.ReleaseId;
import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;
import de.marmaro.krt.ffupdater.metadata.ReleaseVersion;

public class Cache {
    public static final String BASE = "download_metadata_";

    private static final String DOWNLOAD_URL = BASE + "%s_download_url";
    private static final String RELEASE_ID = BASE + "%s_release_id";
    private static final String CREATED_EPOCH_MS = BASE + "%s_created_epoch_ms";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final SharedPreferences preferences;

    Cache(SharedPreferences preferences) {
        Objects.requireNonNull(preferences);
        this.preferences = preferences;
    }

    Optional<AvailableMetadata> getMetadata(App app) {
        return getDownloadUrl(app).flatMap(downloadUrl ->
                getReleaseId(app).map(releaseId ->
                        new AvailableMetadata(releaseId, downloadUrl)));
    }

    private Optional<URL> getDownloadUrl(App app) {
        return getString(DOWNLOAD_URL, app).map(url -> {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                throw new ParamRuntimeException(e, "cached URL '%s' is not valid for app %s", url, app);
            }
        });
    }

    private Optional<ReleaseId> getReleaseId(App app) {
        return getString(RELEASE_ID, app).map(releaseId -> {
            switch (app.getReleaseIdType()) {
                case VERSION:
                    return new ReleaseVersion(releaseId);
                case TIMESTAMP:
                    try {
                        return new ReleaseTimestamp(ZonedDateTime.parse(releaseId));
                    } catch (DateTimeParseException e) {
                        throw new ParamRuntimeException(e, "timestamp '%s' from cache is invalid", releaseId);
                    }
                default:
                    throw new ParamRuntimeException("unknown release id");
            }
        });
    }

    private Optional<String> getString(String keyTemplate, App app) {
        final String key = String.format(keyTemplate, app);
        final String rawValue = preferences.getString(key, null);
        return Optional.ofNullable(rawValue);
    }

    boolean isCacheUpToDate(App app) {
        final String key = String.format(CREATED_EPOCH_MS, app);
        final Duration age = Duration.ofMillis(System.currentTimeMillis() - preferences.getLong(key, 0));
        return new CompareHelper<>(age).isLessOrEqualTo(CACHE_TTL);
    }

    void updateCache(App app, AvailableMetadata metadata) {
        preferences.edit()
                .putLong(String.format(CREATED_EPOCH_MS, app), System.currentTimeMillis())
                .putString(String.format(DOWNLOAD_URL, app), metadata.getDownloadUrl().toString())
                .putString(String.format(RELEASE_ID, app), metadata.getReleaseId().getValueAsString())
                .apply();
    }
}
