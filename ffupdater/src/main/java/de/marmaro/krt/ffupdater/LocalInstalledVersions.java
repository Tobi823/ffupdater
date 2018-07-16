package de.marmaro.krt.ffupdater;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains all version numbers for every installed firefox.
 * Created by Tobiwan on 14.07.2018.
 */
public class LocalInstalledVersions {
    private Map<UpdateChannel, Version> versions = new HashMap<>();

    /**
     * Set the version number for a specific update channel
     * @param updateChannel
     * @param version
     */
    public void setVersion(UpdateChannel updateChannel, Version version) {
        versions.put(updateChannel, version);
    }

    /**
     * Check if specific update channel is installed.
     * @param updateChannel
     * @return
     */
    public boolean isPresent(UpdateChannel updateChannel) {
        return null != versions.get(updateChannel);
    }

    /**
     * Get the version for a specific update channel.
     * @param updateChannel
     * @return
     */
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

        LocalInstalledVersions that = (LocalInstalledVersions) o;

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
        return "LocalInstalledVersions{" +
                "versions=" + versions +
                '}';
    }
}
