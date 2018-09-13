package de.marmaro.krt.ffupdater.version;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created by Tobiwan on 15.07.2018.
 */
public class Version {
    private String name;
    private int code;

    public Version(String name, int code) {
        this.name = name;
        this.code = code;
    }

    /**
     * @return the version name (for example 58.1)
     */
    public String getName() {
        return name;
    }

    /**
     * @return the version code (for example 2015538137)
     */
    public int getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        return new EqualsBuilder()
                .append(code, version.code)
                .append(name, version.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(code)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Version{" +
                "name='" + name + '\'' +
                ", code=" + code +
                '}';
    }
}
