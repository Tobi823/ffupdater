package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Tobiwan on 21.08.2019.
 */
public class AvailableAppsLoader extends AsyncTaskLoader<AvailableApps> {

    private Set<App> appsToCheck;
    private boolean triggerDownload = false;
    private App appToDownload;

    private AvailableAppsLoader(@NonNull Context context, Set<App> appsToCheck) {
        super(context);
        this.appsToCheck = appsToCheck;
    }

    static AvailableAppsLoader onlyCheckAvailableApps(@NonNull Context context, List<App> appsToCheck) {
        return new AvailableAppsLoader(context, new HashSet<>(appsToCheck));
    }

    static AvailableAppsLoader checkAvailableAppsAndTriggerDownload(@NonNull Context context, List<App> appsToCheck, App appToDownload) {
        AvailableAppsLoader availableAppsLoader = onlyCheckAvailableApps(context, appsToCheck);
        availableAppsLoader.triggerDownload = true;
        availableAppsLoader.appToDownload = appToDownload;
        return availableAppsLoader;
    }

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
