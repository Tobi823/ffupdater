package de.marmaro.krt.ffupdater.app.interfaces;

import android.content.Context;

import java.util.Optional;

public interface Display {
    String getDisplayTitle(Context context);

    String getDisplayDescription(Context context);

    Optional<String> getDisplayWarning(Context context);

    Optional<String> getDisplayInstalledVersion(Context context);

    String getDisplayDownloadSource(Context context);
}
