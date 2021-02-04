package de.marmaro.krt.ffupdater.app.interfaces;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceManager;

import org.apache.commons.codec.binary.ApacheCodecHex;

import java.util.Optional;

import de.marmaro.krt.ffupdater.app.interfaces.Compatibility;
import de.marmaro.krt.ffupdater.app.interfaces.Display;
import de.marmaro.krt.ffupdater.app.interfaces.InstallerInfo;
import de.marmaro.krt.ffupdater.app.interfaces.UpdateCheck;

public abstract class BaseApp implements Display, InstallerInfo, Compatibility, UpdateCheck {

    @Override
    public boolean isInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public byte[] getSignatureHash() {
        return ApacheCodecHex.decodeHex(getSignatureHashAsString());
    }

    protected Optional<String> getInstalledVersionFromPackageManager(Context context) {
        try {
            final PackageManager pm = context.getPackageManager();
            final PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return Optional.of(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            return Optional.empty();
        }
    }

    protected Optional<String> getInstalledVersionFromSharedPreferences(Context context, String key) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Optional.ofNullable(preferences.getString(key, null));
    }

    protected void setInstalledVersionInSharedPreferences(Context context, String key, String value) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit()
                .putString(key, value)
                .apply();
    }
}