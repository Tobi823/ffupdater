package de.marmaro.krt.ffupdater.app.interfaces;

import android.content.pm.PackageManager;

import java.util.Optional;

public interface InstallerInfo {
    /**
     * Raw installed version + check if installed
     * @return
     */
    Optional<String> getInstalledVersion(PackageManager pm);
    String getPackageName();
    byte[] getSignatureHash();
    String getSignatureHashAsString();
}
