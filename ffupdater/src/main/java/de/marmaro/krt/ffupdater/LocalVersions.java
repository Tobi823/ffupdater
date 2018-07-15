package de.marmaro.krt.ffupdater;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tobiwan on 14.07.2018.
 */
public class LocalVersions {
    private Map<UpdateChannel, Version> versions = new HashMap<>();

    public void setVersion(UpdateChannel updateChannel, Version version) {
        versions.put(updateChannel, version);
    }

    public boolean isPresent(UpdateChannel updateChannel) {
        return versions.containsKey(updateChannel);
    }

    public Version getVersionString(UpdateChannel updateChannel) {
        Version version = versions.get(updateChannel);
        if (null == version) {
            return new Version("", 0);
        }
        return version;
    }
}
