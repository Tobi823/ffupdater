package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is necessary for executing {@link AvailableApps} asynchronous.
 */
class AvailableAppsAsync extends AsyncTaskLoader<AvailableApps> {
    private final Set<App> appsToCheck;
    private boolean triggerDownload = false;
    private App appToDownload;

    private AvailableAppsAsync(@NonNull Context context, Set<App> appsToCheck) {
        super(context);
        this.appsToCheck = appsToCheck;
    }

    /**
     * Create a AsyncTaskLoader only for retrieving the latest version names.
     * @param context
     * @param appsToCheck
     * @return
     */
    static AvailableAppsAsync onlyCheckAvailableApps(@NonNull Context context, List<App> appsToCheck) {
        return new AvailableAppsAsync(context, new HashSet<>(appsToCheck));
    }

    /**
     * Create a AsyncTaskLoader for retrieving the latest version names and download a specific app.
     * @param context
     * @param appsToCheck
     * @param appToDownload
     * @return
     */
    static AvailableAppsAsync checkAvailableAppsAndTriggerDownload(@NonNull Context context, List<App> appsToCheck, App appToDownload) {
        AvailableAppsAsync availableAppsAsync = onlyCheckAvailableApps(context, appsToCheck);
        availableAppsAsync.triggerDownload = true;
        availableAppsAsync.appToDownload = appToDownload;
        return availableAppsAsync;
    }

    /**
     * This method will be called automatically.
     * @return
     */
    @Nullable
    @Override
    public AvailableApps loadInBackground() {
        if (triggerDownload) {
            return AvailableApps.createAndTriggerDownload(appsToCheck, appToDownload);
        }
        return AvailableApps.create(appsToCheck);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }
}
