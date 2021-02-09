package de.marmaro.krt.ffupdater;

import android.content.SharedPreferences;

import java.util.Objects;

import de.marmaro.krt.ffupdater.metadata.fetcher.Cache;

public class Migrator {
    public static final String FFUPDATER_VERSION_NAME = "migrator_ffupdater_version_name";
    private final SharedPreferences preferences;
    private final String currentVersion;

    public Migrator(SharedPreferences sharedPreferences) {
        this(sharedPreferences, BuildConfig.VERSION_NAME);
    }

    Migrator(SharedPreferences preferences, String currentVersion) {
        Objects.requireNonNull(preferences);
        Objects.requireNonNull(currentVersion);
        this.preferences = preferences;
        this.currentVersion = currentVersion;
    }

    public void migrate() {
        String lastVersion = preferences.getString(FFUPDATER_VERSION_NAME, "");

        if (!currentVersion.equals(lastVersion)) {
            deleteAvailableMetadataCache();
        }

        preferences.edit()
                .putString(FFUPDATER_VERSION_NAME, currentVersion)
                .apply();
    }

    private void deleteAvailableMetadataCache() {
        final SharedPreferences.Editor editor = preferences.edit();
        preferences.getAll().keySet().stream()
                .filter(key -> key.startsWith(Cache.BASE))
                .forEach(editor::remove);
        editor.apply();
    }
}
