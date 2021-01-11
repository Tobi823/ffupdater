package de.marmaro.krt.ffupdater.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.util.Optional;

import de.marmaro.krt.ffupdater.app.interfaces.Compatibility;
import de.marmaro.krt.ffupdater.app.interfaces.Display;
import de.marmaro.krt.ffupdater.app.interfaces.InstallerInfo;
import de.marmaro.krt.ffupdater.app.interfaces.UpdateCheck;

public abstract class BaseApp implements Display, InstallerInfo, Compatibility, UpdateCheck {

    @Override
    public Optional<String> getInstalledVersion(PackageManager pm) {
        try {
            final PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return Optional.of(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public byte[] getSignatureHash() {
        return ApacheCodecHex.decodeHex(getSignatureHashAsString());
    }
}