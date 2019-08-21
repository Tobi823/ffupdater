package de.marmaro.krt.ffupdater;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

/**
 * Created by Tobiwan on 21.08.2019.
 */
public class AvailableAppsLoader extends AsyncTaskLoader<AvailableApps> {

    public AvailableAppsLoader(@NonNull Context context) {
        super(context);
    }

    @Nullable
    @Override
    public AvailableApps loadInBackground() {
        return AvailableApps.create();
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }
}
