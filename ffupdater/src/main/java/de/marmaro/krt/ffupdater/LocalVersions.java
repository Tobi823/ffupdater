package de.marmaro.krt.ffupdater;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        LocalVersions that = (LocalVersions) o;

        return new EqualsBuilder()
                .append(versions, that.versions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(versions)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "LocalVersions{" +
                "versions=" + versions +
                '}';
    }
}
