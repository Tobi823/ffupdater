package de.marmaro.krt.ffupdater.metadata;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

/**
 * Detect installed apps and their version names.
 */
public class InstalledMetadataRegister {
    private static final String KEY = "device_app_register_%s_version_name";

    private final PackageManager packageManager;
    private final SharedPreferences preferences;

    public InstalledMetadataRegister(PackageManager packageManager, SharedPreferences preferences) {
        this.packageManager = packageManager;
        this.preferences = preferences;
    }

    public List<App> getInstalledApps() {
        return Arrays.stream(App.values())
                .filter(this::isInstalled)
                .collect(Collectors.toList());
    }

    public List<App> getNotInstalledApps() {
        return Arrays.stream(App.values())
                .filter(app -> !isInstalled(app))
                .collect(Collectors.toList());
    }

    public boolean isInstalled(App app) {
        try {
            packageManager.getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public Optional<InstalledMetadata> getMetadata(App app) {
        final String versionName;
        try {
            versionName = packageManager.getPackageInfo(app.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return Optional.empty();
        }

        final ReleaseId releaseId;
        switch (app.getReleaseIdType()) {
            case VERSION:
                releaseId = new ReleaseVersion(versionName);
                break;
            case TIMESTAMP:
                final String value = preferences.getString(String.format(KEY, app), null);
                if (value == null) {
                    return Optional.empty();
                }
                releaseId = new ReleaseTimestamp(ZonedDateTime.parse(value));
                break;
            default:
                throw new ParamRuntimeException("unknown release id type");
        }

        return Optional.of(new InstalledMetadata(versionName, releaseId));
    }

    public void saveReleaseId(App app, ReleaseId releaseId) {
        if (app.getReleaseIdType() == App.ReleaseIdType.TIMESTAMP) {
            preferences.edit()
                    .putString(String.format(KEY, app), releaseId.getValueAsString())
                    .apply();
        }
    }
}