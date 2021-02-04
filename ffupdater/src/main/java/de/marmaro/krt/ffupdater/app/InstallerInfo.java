package de.marmaro.krt.ffupdater.app;

import android.content.Context;

import java.util.Optional;

public interface InstallerInfo {
    boolean isInstalled(Context context);
    Optional<String> getInstalledVersion(Context context);
    String getPackageName();
    byte[] getSignatureHash();
    String getSignatureHashAsString();
    void installationCallback(Context context, String installedVersion);
}
