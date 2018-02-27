// https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
package de.marmaro.krt.ffupdater;

import java.io.Serializable;

public class NightlyVersion implements Comparable<NightlyVersion>, Serializable {

    private String nightlyversion;

    public final String get() {
        return this.nightlyversion;
    }

    NightlyVersion(String nightlyversion) {
        if(nightlyversion == null)
            throw new IllegalArgumentException("Version can not be null");
        if(!nightlyversion.matches("[0-9]+(\\.[0-9]+)*"))
            throw new IllegalArgumentException("Invalid version format");
        this.nightlyversion = nightlyversion;
    }

    @Override public int compareTo(NightlyVersion that) {
        if(that == null)
            return 1;
        String[] thisParts = this.get().split("\\.");
        String[] thatParts = that.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override public boolean equals(Object that) {
        if(this == that)
            return true;
        if(that == null)
            return false;
        if(this.getClass() != that.getClass())
            return false;
        return this.compareTo((NightlyVersion) that) == 0;
    }

}
